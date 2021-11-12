# Xiaoyan terncy-sd01 device driver for Hubitat HE #

Driver for the xiaoyan terncy-sd01 (which I purchased off aliexpress: https://www.aliexpress.com/item/4001062350488.html?spm=a2g0o.productlist.0.0.7de72775OoV8Vb&algo_pvid=e0da9b75-ac20-433c-93ed-6c4486982495&algo_exp_id=e0da9b75-ac20-433c-93ed-6c4486982495-0&pdp_ext_f=%7B%22sku_id%22%3A%2210000013985719352%22%7D) (note: I'm not affiliated in any way)

## Device features ##

I have had to reverse engineer the features of the dial, so I may not have captured everything. However, what I do have 
in place is:

**Button tap:** Supports up to 9 x taps each tap will register as a a button from 1 to 9
**Button hold:** Supports up to 8 x holds: button 11 to button 18 **ℹ︎ see below for quirks**
**Rotation direction:** Clockwise and anti-clockwise
**Rotation amount:** A value representing how much the dial was rotated.

**Note:** Rotation direction and amount are only sent once the rotation stops. i.e. rotation doesn't start registering 
at the commencement of rotation, it only registers at the end of the rotation event. This is a hardware thing.

## Driver Features ##

Supports capabilities:
* Configuration
* Battery
* PushableButton
* LevelPreset
* AudioVolume

### LevelPreset ##

Level change supports 3 different units of measure: Division, rotation %, degrees° rotated
Level change can be toggled from an absolute value to a relative value

Volume can be configured with a variable step size to tweak the sensitivity

### PushableButton behaviours ###

The device has a single button which tracks multi-tap up to 9 times each multi tap has been configured to register as a 
button from 1 to 9


### HoldableButton behaviours ###

The device supports button hold up to 8 times but has some quirks:

#### ℹ Quirks ####
Holding the button for ~1 seconds registers as a single button hold (button 11). Holding the button for ~2 seconds 
registers as a button hold for button 11 and then button 12. The driver code hides this behaviour away, so that you only 
see the last button in the sequence. 

The way it does this, is by waiting for an additional 1100ms after a hold is registered, to check if an additional hold 
event is triggered after the current. Therefore, a hold event lasts a second, but the event will not be triggered until 
1100ms + 1000ms x button hold count e.g. If you hold for the third button hold event, it will not be triggered for ~4 
seconds

 
##### Warning ##### 
Button 18 may not be useful and button 17 might be frustrating to try to target, because if you hold the 
button down for too long, you will place the dial in zigbee pairing mode which is about ~8 seconds.

On holding for ~8 seconds, button 18 will be triggered, but you will have to pair your dial with your zigbee controller 
again.


### Custom capability: Rotation change ###

Rotation change supports negative and positive values outside of the standard 0-100 range
Rotation change supports 3 different units of measure: Division, rotation %, degrees° rotated
Rotation change can be toggled between relative and absolute values
Rotation supports a unit multiplier to tweak the sensitivity





## Driver download ##

https://github.com/aaronchilcott/hubitat/blob/main/drivers/xiaoyan/terncy-sd01/xiaoyan%20-%20terncy-sd01%20-%20driver.groovy
