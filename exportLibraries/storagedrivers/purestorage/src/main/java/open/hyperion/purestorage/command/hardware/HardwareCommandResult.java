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

package open.hyperion.purestorage.hardware.command;

public class HardwareCommandResult {
    private String details;
    private String identify;
    private String index;
    private String name;
    private String slot;
    private String speed;
    private String status;
    private String temperature;

    public String getDetails () {
        return this.details;
    }
    public void setDetails (String details) {
        this.details = details;
    }
    public String getIdentify () {
        return this.identify;
    }
    public void setIdentify (String identify) {
        this.identify = identify;
    }
    public String getIndex () {
        return this.index;
    }
    public void setIndex (String index) {
        this.index = index;
    }
    public String getName () {
        return this.name;
    }
    public void setName (String name) {
        this.name = name;
    }
    public String getSlot () {
        return this.slot;
    }
    public void setSlot (String slot) {
        this.slot = slot;
    }
    public String getSpeed () {
        return this.speed;
    }
    public void setSpeed (String speed) {
        this.speed = speed;
    }
    public String getStatus () {
        return this.status;
    }
    public void setStatus (String status) {
        this.status = status;
    }
    public String getTemperature () {
        return this.temperature;
    }
    public void setTemperature (String temperature) {
        this.temperature = temperature;
    }
}