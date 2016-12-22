/*
 * MIT License
 *
 * Copyright (c) 2016 Lavar Askew (open.hyperion@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package open.hyperion.purestorage.command.array;

public class ArrayControllerCommandResult {
    private String mode;
    private String model;
    private String name;
    private String status;
    private String version;

    public String getMode () {
    	return this.mode;
    }
    public void setMode (String mode) {
    	this.mode = mode;
    }
    public String getModel () {
    	return this.model;
    }
    public void set (String model) {
    	this.model = model;
    }
    public String getName () {
    	return this.name;
    }
    public void setName (String name) {
    	this.name = name;
    }
    public String getStatus () {
    	return this.status;
    }
    public void setStatus (String status) {
    	this.status = status;
    }
    public String getVersion () {
    	return this.version;
    }
    public void setVersion (String version) {
    	this.version = version;
    }

    @Override
    public String toString() {
        return "ArrayControllerCommandResult [mode=" + mode +", model=" + model + ", name=" + name + ", status=" + status + ", version=" + version + "]";
    }    
}
