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

import com.google.gson.annotations.SerializedName;

public class ArraySpaceCommandResult {

    private String capacity;

    public void setCapacity (String capacity) {
        this.capacity = capacity;
    }

    public String getCapacity () {
        return capacity;
    }

    private String hostName;

    public void setHostName (String hostName) {
        this.hostName = hostName;
    }

    public String getHostName () {
        return hostName;
    }

    private String snapshots;

    public void setSnapshots (String snapshots) {
        this.snapshots = snapshots;
    }

    public String getSnapshots () {
        return snapshots;
    }

    private String _volumes;

    public void setVolumes (String volumes) {
        this.volumes = volumes;
    }

    public String getVolumes () {
        return volumes;
    }

    @SerializedName("data_reduction")
    private String dataReduction;

    public void setDataReduction (String dataReduction) {
        this.dataReduction = dataReduction;
    }

    public String getDataReduction () {
        return dataReduction;
    }
    
    private String total;

    public void setTotal (String total) {
        this.total = total;
    }

    public String getTotal () {
        return total;
    }

    @SerializedName("shared_space")
    private String sharedSpace;

    public void setSharedSpace (String sharedSpace) {
        this.sharedSpace = sharedSpace;
    }

    public String getSharedSpace () {
        return sharedSpace;
    }

    @SerializedName("thin_provisioning")
    private String thinProvisioning;

    public void setThinProvisioning (String thinProvisioning) {
        this.thinProvisioning = thinProvisioning;
    }

    public String getThinProvisioning () {
        return thinProvisioning;
    }

    @SerializedName("total_reduction")
    private String totalReduction;

    public void setTotalReduction (String totalReduction) {
        this.totalReduction = totalReduction;
    }

    public String getTotalReduction () {
        return totalReduction;
    }

    @Override
    public String toString() {
        return "ArraySpaceCommandResult [capacity=" + capacity + ", dataReduction=" + dataReduction + ", hostName=" + hostName + ", sharedSpace=" + sharedSpace + ", snapshots=" + snapshots + ", volumes=" + volumes + ", total=" + total + ", thinProvisioning=" + thinProvisioning + ", totalReduction=" + totalReduction + "]";
    }    
}
