/*
 * Copyright 2016 Dell Inc. or its subsidiaries.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.emc.sa.service.vipr.oe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.emc.sa.engine.ExecutionUtils;
import com.emc.sa.engine.service.Service;
import com.emc.sa.service.vipr.ViPRExecutionUtils;
import com.emc.sa.service.vipr.ViPRService;
import com.emc.sa.service.vipr.oe.OrchestrationServiceConstants.InputType;
import com.emc.sa.service.vipr.oe.OrchestrationServiceConstants.StepType;
import com.emc.sa.service.vipr.oe.gson.ViprOperation;
import com.emc.sa.service.vipr.oe.gson.ViprTask;
import com.emc.sa.service.vipr.oe.tasks.RunViprREST;
import com.emc.sa.workflow.WorkflowHelper;
import com.emc.storageos.db.client.DbClient;
import com.emc.storageos.model.orchestration.OrchestrationWorkflowDocument;
import com.emc.storageos.model.orchestration.OrchestrationWorkflowDocument.Input;
import com.emc.storageos.model.orchestration.OrchestrationWorkflowDocument.Step;
import com.emc.storageos.model.orchestration.OrchestrationWorkflowDocument.StepAttribute;
import com.emc.storageos.model.orchestration.internal.Primitive;
import com.emc.storageos.model.orchestration.internal.PrimitiveHelper;
import com.emc.storageos.model.orchestration.internal.ViPRPrimitive;
import com.google.gson.Gson;

@Service("OrchestrationService")
public class OrchestrationService extends ViPRService {

    private Map<String, Object> params;
    private String oeOrderJson;

    @Autowired
    private DbClient dbClient;
    
    //<StepId, {"key" : "values...", "key" : "values ..."} ...>
    final private Map<String, Map<String, List<String>>> inputPerStep = new HashMap<String, Map<String, List<String>>>();
    final private Map<String, Map<String, List<String>>> outputPerStep = new HashMap<String, Map<String, List<String>>>();

    final private Map<String, Step> stepsHash = new HashMap<String, OrchestrationWorkflowDocument.Step>();

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OrchestrationService.class);

    private int code;

    @Override
    public void precheck() throws Exception {

        // get input params from order form
        params = ExecutionUtils.currentContext().getParameters();

        // validate input params to insure service will run

    }

    @Override
    public void execute() throws Exception {
        ExecutionUtils.currentContext().logInfo("Starting Orchestration Engine Workflow");
        try {
            wfExecutor();

            ExecutionUtils.currentContext().logInfo("Orchestration Engine Successfully executed Workflow:"
                    + params.get(OrchestrationServiceConstants.WF_ID));
        } catch (final Exception e) {
            ExecutionUtils.currentContext().logError("Orchestration Engine Workflow Failed" + e);
            throw e;
        }
    }


    /**
     * Method to parse Workflow Definition JSON
     *
     * @throws Exception
     */
    public void wfExecutor() throws Exception {

        logger.info("Parsing Workflow Definition");
        
        final String raw = ExecutionUtils.currentContext().getOrder().getWorkflowDocument();
        if( null == raw) {
            throw new IllegalStateException("Invalid orchestration service.  Workflow document cannot be null");
        }
        
        final OrchestrationWorkflowDocument obj = WorkflowHelper.toWorkflowDocument(raw);
        
        ExecutionUtils.currentContext().logInfo("Orchestration Engine Running " +
                "Workflow: " + obj.getName() + "\t Description:" + obj.getDescription());

        final List<Step> steps = obj.getSteps();
        for (Step step : steps)
            stepsHash.put(step.getId(), step);

        Step step = stepsHash.get(StepType.START.toString());
        String next = step.getNext().getDefault();

        while (next != null && !next.equals(StepType.END.toString())) {
            step = stepsHash.get(next);

            ExecutionUtils.currentContext().logInfo("Orchestration Engine Running " +
                    "Step: " + step.getId());

            updateInputPerStep(step);

            //TODO implement waitfortask
            StepAttribute stepAttribute = step.getAttributes();

            String result = null;

            StepType type = StepType.fromString(step.getType());
            Primitive primitive = PrimitiveHelper.get(step.getOperation());
            if( null == primitive) {
                //TODO fail workflow
                throw new IllegalStateException("Primitive not found: " + step.getOperation());
            }
            switch (type) {
                case VIPR_REST: {
                    ExecutionUtils.currentContext().logInfo("Running ViPR REST OpName: " + step.getOperation() + inputPerStep.get(step.getId()));
                    result = ViPRExecutionUtils.execute(new RunViprREST((ViPRPrimitive)(primitive), getClient().getRestClient(), inputPerStep.get(step.getId())));

                    break;
                }
                case REST: {

                    break;
                }
                case ANSIBLE: {
                    ExecutionUtils.currentContext().logInfo("Running Ansible Step");

                    break;
                }
                default:
                    logger.error("Operation Type Not found. Type:{}", step.getType());

                    throw new IllegalStateException(result);
            }

            updateOutputPerStep(step, result);
	    next = updateResult(isSuccess(step, result), result, step);

            if (next == null) {
                ExecutionUtils.currentContext().logError("Orchestration Engine failed to retrieve next step " +
                        "Step: " + step.getId() + ":" + step);

                throw new IllegalStateException(result);
            }
        }
    }

    private boolean isSuccess(Step step, String result)
    {
        if (step.getSuccessCriteria() == null)
            return evaluateDefaultValue(step, code);
        else
            return findStatus(step.getSuccessCriteria(), result);
    }

    private String updateResult(final boolean status, final String result, final Step step) {
        if (status) {
            ExecutionUtils.currentContext().logInfo("Orchestration Engine successfully ran " +
                    "Step: " + step.getId() + ":" + step + "result:" + result);

            return step.getNext().getDefault();
        }

        ExecutionUtils.currentContext().logError("Orchestration Engine failed to run step " +
                "Step: " + step.getId() + ":" + step + "result:" + result);

        return step.getNext().getFailed();

    }

    /**
     * Method to collect all required inputs per step for execution
     *
     * @param step It is the GSON Object of Step
     */
    private void updateInputPerStep(final Step step) throws Exception {
        logger.info("executing Step Id: {} of Type: {}", step.getId(), step.getType());

        Map<String, Input> input = step.getInput();
        if (input == null)
            return;

        final Map<String, List<String>> inputs = new HashMap<String, List<String>>();

        final Iterator it = input.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next().toString();
            Input value = input.get(key);
            
            if (value == null) {
                logger.error("Wrong key for input:{} Can't get input to execute the step:{}", key, step.getId());

                throw new IllegalStateException();
            }

            switch (InputType.fromString(value.getType())) {
                case FROM_USER:
                case OTHERS:
                case ASSET_OPTION: {
                    //TODO handle multiple , separated values
                    final String paramVal = (params.get(key) != null) ? (params.get(key).toString()) : (value.getDefaultValue());

                    if (paramVal == null) {
                        if (value.getRequired()) {
                            logger.error("Can't retrieve input:{} to execute step:{}", key, step.getId());

                            throw new IllegalStateException();
                        }
                        break;
                    }

                    inputs.put(key, Arrays.asList(paramVal));

                    break;
                }
                case FROM_STEP_INPUT:
                case FROM_STEP_OUTPUT: {
                    
                    if(!isValidinput(value))
                    {
                        logger.error("Can't retrieve input:{} to execute step:{}", key, step.getId());
                        
                        throw new IllegalStateException();
                    }

                    if (value.getValue() != null) {
                        final String[] paramVal = value.getValue().split("\\.");
                        final String stepId = paramVal[OrchestrationServiceConstants.STEP_ID];
                        final String attribute = paramVal[OrchestrationServiceConstants.INPUT_FIELD];

                        Map<String, List<String>> stepInput;
                        if (value.getType().equals(InputType.FROM_STEP_INPUT.toString()))
                            stepInput = inputPerStep.get(stepId);
                        else
                            stepInput = outputPerStep.get(stepId);

                        if (stepInput == null) {
                            logger.info("stepInput is null {}", attribute);
                            //TODO waitfortask
                            
                            break;
                            
                        } else {
                            logger.info("value is:{}", stepInput.get(attribute));
                            if (stepInput.get(attribute) != null) {
                                inputs.put(key, stepInput.get(attribute));

                                break;
                            }
                        }   
                    }
                    if (value.getDefaultValue() != null) {
                        inputs.put(key, Arrays.asList(value.getDefaultValue()));
                        logger.info("value default is:{}", Arrays.asList(value.getDefaultValue()));
                        break;
                    }

                    if (false == value.getRequired()) 
                        break;

                    logger.error("Can't retrieve input:{} to execute step:{}", key, step.getId());

                    throw new IllegalStateException();

                }
                default:
                    logger.error("Input Type:{} is Invalid", value.getType());

                    throw new IllegalStateException();
            }
        }

        inputPerStep.put(step.getId(), inputs);
    }
    
    private boolean isValidinput(Input value)
    {
        if (value.getValue() == null && value.getDefaultValue() == null && value.getRequired()) {
            return false;
        }
        
        return true;

    }
    

    /**
     * Parse REST Response and get output values as specified by the user in the workflow definition
     * <p/>
     * Example of Supported output variable formats:
     * "state"
     * "task.resource.id"
     * "task.state"
     *
     * @param step
     * @param result
     */
    private void updateOutputPerStep(final Step step, final String result) throws Exception {
        final Map<String, String> output = step.getOutput();
        if (output == null) 
            return;

        final Map<String, List<String>> out = new HashMap<String, List<String>>();

        Set keyset = output.keySet();
        Iterator it = keyset.iterator();
        while (it.hasNext()) {
            String key = it.next().toString();
            String value = output.get(key);
            out.put(key, evaluateValue(result, value));
        }

        outputPerStep.put(step.getId(), out);
    }

    /**
     * Evaluate
     *
     * @param step
     * @param returnCode
     * @return
     */
    private boolean evaluateDefaultValue(final Step step, final int returnCode) {
        if (step.getType().equals(StepType.ANSIBLE.toString())) {
            if (returnCode == 0)
                return true;

            return false;
        }

        String opName = step.getOperation();
        //TODO get returncode for REST API from DB. Now it is hard coded.
        int code = 200;
        if (returnCode == code)
            return true;

        return false;
    }

    /**
     * This evaluates the expression from ViPR GSON structure.
     * e.g: It evaluates "task.resource.id" from ViPR REST Response
     *
     * @param result
     * @param value
     * @return
     */
    private List<String> evaluateValue(final String result, String value) throws Exception {

        final Gson gson = new Gson();
        ViprOperation res = gson.fromJson(result, ViprOperation.class);
        ExpressionParser parser = new SpelExpressionParser();

        logger.debug("Find value of:{}", value);
        List<String> valueList = new ArrayList<String>();

        if (!value.contains(OrchestrationServiceConstants.TASK)) {
            Expression expr = parser.parseExpression(value);
            EvaluationContext context = new StandardEvaluationContext(res);
            String val = (String) expr.getValue(context);

            valueList.add(val);

        } else {

            String[] values = value.split("task.", 2);
            if (values.length != 2) {
                logger.error("Cannot evaluate values with statement:{}", value);

                throw new IllegalStateException();
            }
            value = values[1];
            Expression expr = parser.parseExpression(value);

            ViprTask[] tasks = res.getTask();
            for (ViprTask task : tasks) {
                EvaluationContext context = new StandardEvaluationContext(task);
                String v = (String) expr.getValue(context);
                valueList.add(v);
            }

            logger.info("valueList is:{}", valueList);
        }
        
        return valueList;
    }

    /**
     * This evaluates the status of a step from the SuccessCriteria mentioned in workflow definition JSON
     * e.g: Supported Expression Language for SuccessCriteria
     * Supported condition type code == x [x can be any number]
     * "returnCode == 404"
     * "returnCode == 0"
     * "task_state == 'pending' and description == 'create export1' and returnCode == 400"
     * "state == 'ready'";
     * Note: and, or cannot be part of lvalue or rvalue
     *
     * @param successCriteria
     * @param result
     * @return
     */
    private boolean findStatus(String successCriteria, final String result) {
        try {

            if (successCriteria == null)
                return true;

            if (successCriteria != null && result == null)
                return false;

            SuccessCriteria sc = new SuccessCriteria();
            ExpressionParser parser = new SpelExpressionParser();
            EvaluationContext con2 = new StandardEvaluationContext(sc);
            String[] statements = successCriteria.split("\\bor\\b|\\band\\b");

            if (statements.length == 0)
                return false;

            int p = 0;
            for (String statement : statements) {

                if (statement.startsWith(OrchestrationServiceConstants.RETURN_CODE)) {
                    Expression e2 = parser.parseExpression(statement);

                    boolean val = e2.getValue(con2, Boolean.class);
                    logger.info("Evaluated value for errorCode or returnCode is:{}", val);

                    successCriteria = successCriteria.replace(statement, " " + val + " ");

                    continue;
                }

                String arr[] = StringUtils.split(statement);
                String lvalue = arr[0];

                if (!lvalue.contains(OrchestrationServiceConstants.TASK)) {

                    List<String> evaluatedValues = evaluateValue(result, lvalue);
                    sc.setEvaluateVal(evaluatedValues.get(0), p);
                    successCriteria = successCriteria.replace(lvalue, " evaluateVal[" + p + "]");
                    p++;

                    continue;
                }

                //TODO accepted format is task_state but spel expects task.state. Could not find a regex for that
                String lvalue1 = lvalue.replace("_", ".");

                List<String> evaluatedValues = evaluateValue(result, lvalue1);

                boolean val2 = true;

                if (evaluatedValues.isEmpty())
                    return false;

                String exp1 = statement.replace(lvalue, "eval");
                for (String evaluatedValue : evaluatedValues) {
                    sc.setEval(evaluatedValue);
                    Expression e = parser.parseExpression(exp1);
                    val2 = val2 && e.getValue(con2, Boolean.class);
                }

                successCriteria = successCriteria.replace(statement, " " + val2 + " ");
            }

            logger.info("Success Criteria to evaluate:{}", successCriteria);
            Expression e1 = parser.parseExpression(successCriteria);
            boolean val1 = e1.getValue(con2, Boolean.class);

            logger.info("Evaluated Value is:{}" + val1);

            return val1;
        } catch (final Exception e) {
            logger.error("Cannot evaluate success Criteria:{} Exception:{}", successCriteria, e);

            return false;
        }
    }
}