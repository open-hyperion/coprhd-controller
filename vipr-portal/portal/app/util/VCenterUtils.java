/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package util;

import com.emc.storageos.db.client.util.NullColumnValueGetter;
import com.emc.storageos.model.auth.ACLAssignmentChanges;
import com.emc.storageos.model.auth.ACLAssignments;
import com.emc.storageos.model.auth.ACLEntry;
import com.emc.storageos.model.host.cluster.ClusterRestRep;
import com.emc.storageos.model.host.vcenter.VcenterCreateParam;
import com.emc.storageos.model.host.vcenter.VcenterDataCenterRestRep;
import com.emc.storageos.model.host.vcenter.VcenterRestRep;
import com.emc.storageos.model.host.vcenter.VcenterUpdateParam;
import com.emc.vipr.client.Task;
import com.emc.vipr.client.exceptions.TimeoutException;
import com.emc.vipr.client.exceptions.ViPRException;
import com.emc.vipr.client.exceptions.ViPRHttpException;
import controllers.security.Security;
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.emc.vipr.client.core.util.ResourceUtils.id;
import static com.emc.vipr.client.core.util.ResourceUtils.mapById;
import static com.emc.vipr.client.core.util.ResourceUtils.uri;
import static util.BourneUtil.getViprClient;

public class VCenterUtils {

    public static List<VcenterRestRep> getVCenters(URI tenantId) {
        return getViprClient().vcenters().getVcenters(tenantId);
    }

    public static VcenterRestRep getVCenter(URI id) {
        try {
            return (id != null) ? getViprClient().vcenters().get(id) : null;
        } catch (ViPRHttpException e) {
            if (e.getHttpCode() == 404) {
                return null;
            }
            throw e;
        }
    }

    public static VcenterDataCenterRestRep getDataCenter(URI id) {
        try {
            return (id != null) ? getViprClient().vcenterDataCenters().get(id) : null;
        } catch (ViPRHttpException e) {
            if (e.getHttpCode() == 404) {
                return null;
            }
            throw e;
        }
    }

    public static VcenterRestRep getVCenter(VcenterDataCenterRestRep dataCenter) {
        return (dataCenter != null) ? getVCenter(id(dataCenter.getVcenter())) : null;
    }

    public static Task<VcenterRestRep> createVCenter(URI tenantId, VcenterCreateParam vcenterCreateParam, boolean validateConnection) {
        return getViprClient().vcenters().create(tenantId, vcenterCreateParam, validateConnection);
    }

    public static Task<VcenterRestRep> updateVCenter(URI vcenterId, VcenterUpdateParam vcenterUpdateParam, boolean validateConnection) {
        return getViprClient().vcenters().update(vcenterId, vcenterUpdateParam, validateConnection);
    }

    public static Task<VcenterRestRep> deactivateVCenter(URI id) {
        return getViprClient().vcenters().deactivate(id);
    }

    public static Task<VcenterRestRep> deactivateVCenter(URI vcenterId, boolean detachStorage) {
        return getViprClient().vcenters().deactivate(vcenterId, detachStorage);
    }

    public static Task<VcenterRestRep> detachStorage(URI vcenterId) {
        return getViprClient().vcenters().detachStorage(vcenterId);
    }

    public static List<VcenterDataCenterRestRep> getDataCentersInVCenter(VcenterRestRep vcenter, URI tenantId) {
        return getViprClient().vcenterDataCenters().getByVcenter(id(vcenter), tenantId);
    }

    public static List<VcenterDataCenterRestRep> getDataCentersInVCenter(VcenterRestRep vcenter) {
        return getViprClient().vcenterDataCenters().getByVcenter(id(vcenter), null);
    }

    public static List<ClusterRestRep> getClustersInDataCenter(VcenterDataCenterRestRep vcenterDataCenter) {
        return getViprClient().clusters().getByDataCenter(id(vcenterDataCenter));
    }

    public static Task<VcenterRestRep> discover(URI id) {
        return getViprClient().vcenters().discover(id);
    }

    public static boolean checkCompatibleVDCVersion(String expectedVersion) {
        return getViprClient().vdcs().isCompatibleVDCVersion(expectedVersion);
    }

    public static List<ACLEntry> getAcl(URI vCenterId) {
        try {
            return (vCenterId != null) ? getViprClient().vcenters().getAcls(vCenterId) : null;
        } catch (ViPRHttpException e) {
            if (e.getHttpCode() == 404) {
                return null;
            }
            throw e;
        }
    }

    public static Task<VcenterRestRep> createVCenter(VcenterCreateParam vcenterCreateParam, boolean validateConnection,
                                                     ACLAssignmentChanges aclAssignmentChanges) {
        Task<VcenterRestRep> vCenterCreateTask = getViprClient().vcenters().create(vcenterCreateParam, validateConnection);
        if (canUpdateACLs() && isValidAclAssignments(aclAssignmentChanges)) {
            return updateAcl(aclAssignmentChanges, vCenterCreateTask);
        } else {
            return vCenterCreateTask;
        }
    }

    public static Task<VcenterRestRep> updateVCenter(URI vcenterId, VcenterUpdateParam vcenterUpdateParam,
                                                     boolean validateConnection, ACLAssignmentChanges aclAssignmentChanges,
                                                     boolean discoverVcenter) {
        Task<VcenterRestRep> vCenterUpdateTask = getViprClient().vcenters().update(vcenterId, vcenterUpdateParam, validateConnection, discoverVcenter);
        if (canUpdateACLs() && isValidAclAssignments(aclAssignmentChanges)) {
            return updateAcl(vcenterId, aclAssignmentChanges);
        } else {
            return vCenterUpdateTask;
        }
    }

    private static Task<VcenterRestRep> updateAcl(ACLAssignmentChanges aclAssignmentChanges, Task<VcenterRestRep> vCenterTask) {
        if (canUpdateACLs() && isValidAclAssignments(aclAssignmentChanges)) {
            VcenterRestRep vCenter = vCenterTask.get();
            return updateAcl(vCenter.getId(), aclAssignmentChanges);
        }
        return vCenterTask;
    }

    public static Task<VcenterRestRep> updateAcl(URI vCenterId, ACLAssignmentChanges aclAssignmentChanges) {
        if (canUpdateACLs() && isValidAclAssignments(aclAssignmentChanges)) {
            return getViprClient().vcenters().updateAcls(vCenterId, aclAssignmentChanges);
        }
        return null;
    }

    public static boolean canUpdateACLs() {
        return Security.hasAnyRole(Security.SECURITY_ADMIN, Security.SYSTEM_ADMIN);
    }

    private static boolean isValidAclAssignments (ACLAssignmentChanges aclAssignmentChanges) {
        if (!(CollectionUtils.isEmpty(aclAssignmentChanges.getAdd()) &&
                CollectionUtils.isEmpty(aclAssignmentChanges.getRemove()))) {
            return true;
        }
        return false;
    }
}
