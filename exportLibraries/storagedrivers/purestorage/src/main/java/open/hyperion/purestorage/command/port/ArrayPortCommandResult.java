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

package open.hyperion.purestorage.command.port;

import com.google.gson.annotations.SerializedName;

public class ArrayPortCommandResult {
    private String portal;
    @SerializedName("target_iqn")
    private String targetIqn;
    private String target;
    @SerializedName("target_portal")
    private String targetPortal;
    @SerializedName("target_wwn")
    private String targetWwn;
    private String wwn;

    public String getTarget() {
        return this.target;
    }
    public void setTarget(String target) {
        this.target = target;
    }
    public String getTargetIqn() {
        return this.targetIqn;
    }
    public void setTargetIqn(String targetIqn) {
        this.targetIqn = targetIqn;
    }
    public String getPortal() {
        return this.portal;
    }
    public void setPortal(String portal) {
        this.portal = portal;
    }
    public String getTargetPortal() {
        return this.targetPortal;
    }
    public void setTargetPortal(String targetPortal) {
        this.targetPortal = targetPortal;
    }
    public String getWwn() {
        return this.wwn;
    }
    public void setWwn(String wwn) {
        this.wwn = wwn;
    }
    public String getTargetWwn() {
        return this.targetWwn;
    }
    public void setTargetWwn(String targetWwn) {
        this.targetWwn = targetWwn;
    }
}