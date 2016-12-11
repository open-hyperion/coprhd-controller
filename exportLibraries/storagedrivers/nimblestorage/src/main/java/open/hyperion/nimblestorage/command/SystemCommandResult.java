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

package open.hyperion.nimblestorage.command;

public class SystemCommandResult {
    private String _name;
    private String _systemVersion;
    private String _model;
    private String _serialNumber;
    private String _totalNodes;
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        _name = name;
    }
    public String getSystemVersion() {
        return _systemVersion;
    }
    public void setSystemVersion(String systemVersion) {
        _systemVersion = systemVersion;
    }
    public String getModel() {
        return _model;
    }
    public void setModel(String model) {
        _model = model;
    }
    public String getSerialNumber() {
        return _serialNumber;
    }
    public void setSerialNumber(String serialNumber) {
        _serialNumber = serialNumber;
    }
    public String getTotalNodes() {
        return _totalNodes;
    }
    public void setTotalNodes(String totalNodes) {
        _totalNodes = totalNodes;
    }
    
    @Override
    public String toString() {
        return "SystemCommandResult [name=" + _name + ", systemVersion=" + _systemVersion + ", model=" + _model + ", serialNumber="
                + _serialNumber + ", totalNodes=" + _totalNodes + "]";
    }
}
