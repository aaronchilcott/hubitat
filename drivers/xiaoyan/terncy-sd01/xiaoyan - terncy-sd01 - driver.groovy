/**

Xiaoyan TERNCY-SD01 Dial driver for Hubitat Elevation >= v2.0.0

Copyright © 2021 Aaron Chilcott, siaison pty ltd

MIT License (MIT)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import hubitat.zigbee.zcl.DataType

metadata {


    definition (name: getModelName(), namespace: "siaison.co", author: "Aaron Chilcott / siaison") {

        capability "Configuration"
        capability "Battery"

        capability "PushableButton"
        capability "LevelPreset"
        capability "AudioVolume"


        command "push", ["NUMBER"]
        command "hold", ["NUMBER"]
        command "rotate", ["NUMBER", "NUMBER"]

        command "presetLevel", ["NUMBER"]

        command "mute"
        command "setVolume", ["NUMBER"]
        command "unmute"
        command "volumeUp"
        command "volumeDown"


        attribute "rotationAmount", "NUMBER"
        attribute "rotationDirection", "STRING"

        attribute "whenLastButtonPushed", "NUMBER"
        attribute "whenLastButtonHeld", "NUMBER"




/* Clusters:
Reference docs:
https://zigbeealliance.org/wp-content/uploads/2019/12/07-5123-06-zigbee-cluster-library-specification.pdf
Chapter 3: General, section 3.1.2: Cluster list

*/

        fingerprint inClusters: "0000,0001,0003,0020,FCCC", outClusters: "0019, 0006", manufacturer: getManufacturer(), model: getModel()

    }

	preferences {
		input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true


        input (name: "levelUnitOfMeasure",
            title: "Level change: Unit of measure",
            description: "The unit the value of the level change is measured in.",
            type: "enum",
            options: [
                "${LEVEL_UNIT_OF_MEASURE.PERCENT.UNIT}": LEVEL_UNIT_OF_MEASURE.PERCENT.LABEL,
                "${LEVEL_UNIT_OF_MEASURE.DEGREES.UNIT}": LEVEL_UNIT_OF_MEASURE.DEGREES.LABEL,
                "${LEVEL_UNIT_OF_MEASURE.DIVISIONS.UNIT}": LEVEL_UNIT_OF_MEASURE.DIVISIONS.LABEL
            ],
            defaultValue: LEVEL_UNIT_OF_MEASURE.DEGREES.UNIT)


        input (name: "isLevelPresetChangeRelative",
            title: "Level change: Relative increments",
            description: "The value of levelPreset represents the relative change from the last value.",
            type: "enum",
            options: [
                "yes": "Yes",
                "no": "No",
            ],
            defaultValue: true)


        input ( name: "volumeStepSize",
            title: "Volume change: Step size",
            description: "The step size of a single volume increment, the smallest possible change when the dial is turned",
            type: "enum",
            options: AVAILABLE_VOLUME_STEPS,
            defaultValue: true)


        input (name: "rotationUnitOfMeasure",
            title: "Rotation change: Unit of measure",
            description: "The unit the value of the rotation change is measured in.",
            type: "enum",
            options: [
                "${LEVEL_UNIT_OF_MEASURE.PERCENT.UNIT}": LEVEL_UNIT_OF_MEASURE.PERCENT.LABEL,
                "${LEVEL_UNIT_OF_MEASURE.DEGREES.UNIT}": LEVEL_UNIT_OF_MEASURE.DEGREES.LABEL,
                "${LEVEL_UNIT_OF_MEASURE.DIVISIONS.UNIT}": LEVEL_UNIT_OF_MEASURE.DIVISIONS.LABEL
            ],
            defaultValue: LEVEL_UNIT_OF_MEASURE.DEGREES.UNIT)


        input (name: "isRotationChangeRelative",
            title: "Rotation change: Relative increments",
            description: "The value of rotation represents the relative change from the last value.",
            type: "enum",
            options: [
                "yes": "Yes",
                "no": "No",
            ],
            defaultValue: true)


        input (name: "rotationMultiplier",
            title: "Rotation change: Unit multiplier",
            description: "Multiplies the value of rotation.",
            type: "number",
            defaultValue: 1)
	}

}

private getModel() { "TERNCY-SD01" }

private getModelName() { "Terncy-SD01 Dial" }

private getVERSION() { "v0.1HE-beta" }

private getManufacturer() { "Xiaoyan" }

void push(buttonId){
    sendButtonEvent(buttonId, BUTTON_STATES.PUSHED, EVENT_TYPE.DIGITAL)
}

void push(){
    sendButtonEvent(0, BUTTON_STATES.PUSHED, EVENT_TYPE.DIGITAL)
}

void hold(buttonId){
    sendButtonEvent(buttonId, BUTTON_STATES.HELD, EVENT_TYPE.DIGITAL)
}

void rotate(direction, amount){
    def buttonId = 0

    if (direction == ROTATION_DIRECTION.CLOCKWISE.ID) {
        buttonId = DEVICE_BUTTONS.ROTATE_CW.ID
    } else if (direction == ROTATION_DIRECTION.ANTI_CLOCKWISE.ID) {
        buttonId = DEVICE_BUTTONS.ROTATE_ACW.ID
    } else {
        ERROR("Unsupported direction ${direction}, must be either 1 or -1")
        return
    }

    DEBUG("rotate() direction: ${direction} amount: ${amount} buttonId (derived): ${buttonId}")

    triggerRotationEvent(buttonId, BUTTON_STATES.ROTATED, amount, EVENT_TYPE.DIGITAL)
}

void presetLevel(level) {
    setLevelBy(level, levelUnitOfMeasure, EVENT_TYPE.DIGITAL)
}

void mute() {
    setVolumeMuteState(true, EVENT_TYPE.DIGITAL)
}

void setVolume(level) {
    setVolumeLevel(level, EVENT_TYPE.DIGITAL)
}

void unmute() {
    setVolumeMuteState(false, EVENT_TYPE.DIGITAL)
}

void volumeDown() {
    moveVolume(ROTATION_DIRECTION.ANTI_CLOCKWISE.ID, EVENT_TYPE.DIGITAL)
}

void volumeUp() {
    moveVolume(ROTATION_DIRECTION.CLOCKWISE.ID, EVENT_TYPE.DIGITAL)
}

private getCLUSTER_GROUPS() { 0xFCCC }

private getEVENT_TYPE() {
    [
        DIGITAL: "digital",
        ANALOGUE: "analogue",
    ]
}

private getROTATION_DIRECTION() {
    [
        CLOCKWISE: [
            ID: 1,
            STRING_ID: 'clockwise',
        ],
        ANTI_CLOCKWISE: [
            ID: -1,
            STRING_ID: 'antiClockwise',
        ],
    ]
}

private getRotationById(id) {
    def rotation

    try {

        rotation = ROTATION_DIRECTION.find({ it.value.ID == id }).value

    } catch(e) {
        ERROR("Unable to get rotation by ID: ${id}. ${e.message}")
    }

    return rotation
}

private getLEVEL_UNIT_OF_MEASURE() {
    [
        PERCENT: [
            UNIT: "%",
            LABEL: "Percentage of a full dial rotation (%)",
        ],
        DEGREES: [
            UNIT: "°",
            LABEL: "Degrees of rotation, one full turn is 360° (°)",
        ],
        DIVISIONS: [
            UNIT: "divisions",
            LABEL: "Smallest division (30 per turn) # ",
        ]
    ]
}

def getNumberOfButtons(){

    // Push [1-9]    x 9 buttons + counter of how many times pressed in lifetime
    //     Note: Push buttons include a lifetime counter of how many times they
    //        have been pressed, at the time of writing is wasn't clear what
    //        the max value might be.
    //        Each press count will be treated as an independent button.
    //            e.g. 1 x press = button1, 2 x press = button 2 ect up to button 9
    // Hold [11-18]    x 1 button + counter of how many times pressed in session
    //               (sequentially in short period after each other)
    //     NOTE: repeated hold presses are called sequentially one after the other
    //         e.g. To get hold x 2 to trigger, hold x 1, will be triggered first
    //         To get hold x 3 to trigger, hold x 1 & then hold x 2 will be
    //         triggered first.
    //         Initially this driver will count sequential presses as the same
    //         button with a count of how many times it was pressed in a row
    // Rotate left [20]  x 1 button + rotation amount value
    // Rotate right [50] x 1 button + rotation amount value
    //     Note:  1. Rotation is triggered after the dial stops rotating.
    //            2. Rotation events include a value which will be a multiple of 12
    //            3. Each multiple of 12 is equivalent to the tactile resistance/bump
    //               felt when rotating the dial the smallest increment.
    //            4. There are 30 bumps around the dial & 30 x 12 = 360 so each bump
    //               is 12 degrees of rotation, 30 bumps is 360 degrees of rotation


	def numberOfButtons = 19

    DEBUG "There are $numberOfButtons buttons reported. IDs: 1-9: (Push) 11-18: (Hold), 20: (rotate clockwise), 50: (rotate anti-clockwise)"

	return numberOfButtons
}

private getDEVICE_BUTTONS() {
  [
    PRESS_X1: [ ID: 1, NAME: "press x 1", LABEL: "Button tap x 1" ],
    PRESS_X2: [ ID: 2, NAME: "press x 2", LABEL: "Button tap x 2" ],
    PRESS_X3: [ ID: 3, NAME: "press x 3", LABEL: "Button tap x 3" ],
    PRESS_X4: [ ID: 4, NAME: "press x 4", LABEL: "Button tap x 4" ],
    PRESS_X5: [ ID: 5, NAME: "press x 5", LABEL: "Button tap x 5" ],
    PRESS_X6: [ ID: 6, NAME: "press x 6", LABEL: "Button tap x 6" ],
    PRESS_X7: [ ID: 7, NAME: "press x 7", LABEL: "Button tap x 7" ],
    PRESS_X8: [ ID: 8, NAME: "press x 8", LABEL: "Button tap x 8" ],
    PRESS_X9: [ ID: 9, NAME: "press x 9", LABEL: "Button tap x 9" ],
    HOLD_X1: [ ID: 11, NAME: "hold x 1", LABEL: "Button hold x 1", DATA_ID: 1 ],
    HOLD_X2: [ ID: 12, NAME: "hold x 2", LABEL: "Button hold x 2", DATA_ID: 2 ],
    HOLD_X3: [ ID: 13, NAME: "hold x 3", LABEL: "Button hold x 3", DATA_ID: 3 ],
    HOLD_X4: [ ID: 14, NAME: "hold x 4", LABEL: "Button hold x 4", DATA_ID: 4 ],
    HOLD_X5: [ ID: 15, NAME: "hold x 5", LABEL: "Button hold x 5", DATA_ID: 5 ],
    HOLD_X6: [ ID: 16, NAME: "hold x 6", LABEL: "Button hold x 6", DATA_ID: 6 ],
    HOLD_X7: [ ID: 17, NAME: "hold x 7", LABEL: "Button hold x 7", DATA_ID: 7 ],
    HOLD_X8: [ ID: 18, NAME: "hold x 8", LABEL: "Button hold x 8", DATA_ID: 8 ],
    ROTATE_CW: [ ID: 20, NAME: "rotate clockwise", LABEL: "Rotate clockwise" ],
    ROTATE_ACW: [ ID: 50, NAME: "rotate anti-clockwise", LABEL: "Rotate anti-clockwise" ],
  ]
}

private getBUTTON_STATES() {
    [
        PUSHED: "pushed",
        HELD: "held",
        ROTATED: "rotated",
    ]
}

private getButtonById(buttonId) {
    def button = null

    try {
        button = DEVICE_BUTTONS.find { it.value.ID == buttonId }
        button = button.value
    } catch(e) {
        ERROR("Unable to get button by ID: ${buttonId}. ${e.message}")
    }

    return button
}

private getButtonByDataId(dataId) {
    def button = null

    try {
        button = DEVICE_BUTTONS.find { it.value.DATA_ID != null && it.value.DATA_ID == dataId }
        button = button.value
    } catch(e) {
        ERROR("Unable to get button by DATA ID: ${dataId}. ${e.message}")
    }

    return button
}

private getButtonName(buttonId) {

    def button = getButtonById(buttonId)

    return button.name
}

private getButtonLabel(buttonNum) {
    def button = getButtonById(buttonId)

    return button.label
}

private getAVAILABLE_VOLUME_STEPS() {
    def list = [:]

    def myInt = 1

    while (myInt < 51) {
        list.putAt("${myInt}", myInt)
        myInt ++
    }

    return list

}

private getDEVICE_CLUSTERS() {
    [
        Basic: 0x0000,
        PowerConfiguration: 0x0001,
        Identify: 0x0003,
        OnOff: 0x0006,
        OTAUpgrade: 0x0019,
        PollControl: 0x0020,
        DeviceCustom: 0xFCCC,
    ]
}

private getDEVICE_CLUSTER_COMMANDS() {
    [
        DeviceCustom: [
            Push: "00",
            Rotate: "0A",
            Hold: "14",
        ],
    ]
}

private getDeviceInfoAttributeName() {
	[
		"ZCLVersion",
		"ApplicationVersion",
		"StackVersion",
		"HWVersion",
		"ManufacturerName",
		"ModelIdentifier",
		"DateCode",
		"PowerSource",
	]
}

private def setDefaults(reset) {
    sendEvent(name: "numberOfButtons", value: getNumberOfButtons(), isStateChange: true)
    sendEvent(name: "levelPreset", value: device.currentValue("levelPreset") != null && !reset ? device.currentValue("levelPreset") : 0, isStateChange: true)
    sendEvent(name: "volume", value: device.currentValue("volume") != null && !reset ? device.currentValue("volume") : 0, isStateChange: true)
    sendEvent(name: "rotationAmount", value: device.currentValue("rotationAmount") != null && !reset ? device.currentValue("rotationAmount") : 0, isStateChange: true)
    sendEvent(name: "rotationDirection", value: device.currentValue("rotationDirection") != null && !reset ? device.currentValue("rotationDirection") : ROTATION_DIRECTION.CLOCKWISE.String_ID, isStateChange: true)
}

def installed() {
    DEBUG "installed() called - Driver ${VERSION}"

    setDefaults(true)
}

def updated() {
	DEBUG "Updated() called - Driver ${VERSION}"

    setDefaults()

}

def configure() {
    DEBUG "Configuring device ${device.getDataValue("model")} - Driver ${VERSION}"

    setDefaults(true)

	def cmds = zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, 0x21, DataType.UINT8, 30, 21600, 0x01) +
				zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x21) +
		["zdo bind 0x${device.deviceNetworkId} 0x01 0x01 0x500 {${device.zigbeeId}} {}"] +
				readDeviceBindingTable() // Need to read the binding table to see what group it's using

    cmds
}

def INFO(String msg) { if (logEnable) log.info("${msg}")}
def DEBUG(String msg) { if (logEnable) log.debug("${msg}")}
def ERROR(String msg) { if (logEnable) log.error("${msg}")}


// parse events into attributes
def parse(String description) {
    // DEBUG("Parsing message from device: '$description'")

    def event = ""

    try {
        event = zigbee.getEvent(description)
    } catch(e) {
        ERROR("Error caught ${e.message} ${e}")
    }

    if (event) {
        DEBUG("Creating event: ${event}")
        sendEvent(event)
    } else {

        try {
            def tmp = zigbee.parseDescriptionAsMap(description)

            // DEBUG("Description: $description}")
        } catch (e) {
            ERROR("Failed to parse description ${e.message}")
        }

        if ((description?.startsWith("catchall:")) || (description?.startsWith("read attr -"))) {
            // DEBUG("  Catch all: $description")

            def descMap = zigbee.parseDescriptionAsMap(description)

            DEBUG("DESCMAP: ${descMap}")

            if (descMap.clusterInt == DEVICE_CLUSTERS.DeviceCustom && descMap.command  == DEVICE_CLUSTER_COMMANDS.DeviceCustom.Push) {

                DEBUG("Push button")
                event = getButtonEvent(descMap)

            } else if (descMap.clusterInt == DEVICE_CLUSTERS.DeviceCustom && descMap.command == DEVICE_CLUSTER_COMMANDS.DeviceCustom.Hold) {

                DEBUG("Hold button")
                event = getButtonEvent(descMap)

            } else if (descMap.clusterInt == DEVICE_CLUSTERS.DeviceCustom && descMap.command == DEVICE_CLUSTER_COMMANDS.DeviceCustom.Rotate) {

                DEBUG("Rotate button")
                event = getButtonEvent(descMap)

			} else if (descMap.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER && descMap.attrId == "0021") {

				DEBUG("Battery Event")
				event = getBatteryEvent(zigbee.convertHexToInt(descMap.value))

            } else if (descMap.clusterInt == DEVICE_CLUSTERS.Basic && descMap.command == "01") {

				DEBUG("Received Basic Device Info Response")

			} else if (descMap.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER && descMap.command == "07") {

				DEBUG("Received Configure Reporting Response")

			} else if (descMap.clusterInt == zigbee.IDENTIFY_CLUSTER && descMap.command == "01") {

                DEBUG("Received IDENTIFY report")

			} else if (descMap.clusterInt == DEVICE_CLUSTERS.OnOff && descMap.command == "00") {

				DEBUG("Received Descriptor Match Request")

			} else if (descMap.clusterInt == DEVICE_CLUSTERS.OTAUpgrade && descMap.command == "00" && descMap.profileId == "0000") {

				def data = descMap.data
				def id16 = "${data[2]}${data[1]}"
				def id64 = "${data[10]}${data[9]}${data[8]}${data[7]}${data[6]}${data[5]}${data[4]}${data[3]}"
				def caps = "${data[11]}"
				DEBUG("Received DEVICE DISCOVERY BROADCAST: ${id16}  ${id64}  ${caps}")

      } else if (descMap.clusterInt == DEVICE_CLUSTERS.PollControl && descMap.command == "00") {

				DEBUG("Received Poll controll")

			} else {
				DEBUG("UNHANDLED REPORT: \ndescription: ${description}\ndescMap: ${descMap}")
			}
        }

        def result = []
        if (event) {
            DEBUG("Creating event: ${event}")
            result = createEvent(event)
        }

        return result
    }
}


private Integer getVolumeLevel() {
    def currentVolume = device.currentValue("volume")
    def volume = currentVolume ? currentVolume : 0;

    DEBUG "Getting volume ${volume}"

    return volume as Integer
}

private Integer getVolumeStepSize() {
    def steps = volumeStepSize ? volumeStepSize : 0;

    return steps as Integer
}

private void setVolumeMuteState(isMuted, type) {
    def descriptionText = "$device.displayName volume was set with a value of ${volume}"

    DEBUG "${isMuted ? "Muting" : "Un-muting"} volume"
    sendEvent(name: "mute", value: isMuted, type: type, descriptionText: descriptionText, isStateChange: true)
}

private void setVolumeLevel(volume, type) {


    if (volume < 0) {
        volume = 0
    }

    if (volume > 100) {
        volume = 100
    }

    def descriptionText = "setVolumeLevel() triggered for ${device.displayName} volume: ${volume} type: ${type}"

    DEBUG(descriptionText)

    sendEvent(name: "volume", value: volume, unit: LEVEL_UNIT_OF_MEASURE.PERCENT.UNIT, type: type, descriptionText: descriptionText, isStateChange: true)
}

private void moveVolume(direction, type) {
    def volume = getVolumeLevel()
    def newVolume = 0
    def steps = getVolumeStepSize()


    if (direction > 0) {
        newVolume = volume + steps
    } else {
        newVolume = volume - steps
    }

    DEBUG "moveVolume() triggered for ${device.displayName} ${direction > 0 ? "Increasing" : "Decreasing" } volume from ${volume} with a step size of ${steps} to ${newVolume}"

    setVolumeLevel(newVolume, type)
}


private void moveVolumeBy(amount, direction, type) {
    def volume = getVolumeLevel()
    def newVolume = 0


    if (direction == ROTATION_DIRECTION.CLOCKWISE.ID) {
        newVolume = volume + amount
    } else if (direction == ROTATION_DIRECTION.ANTI_CLOCKWISE.ID) {
        newVolume = volume - amount
    } else {
        ERROR ("Unsupported rotation direction: ${direction}")
        return
    }

    DEBUG "moveVolumeBy() triggered for ${device.displayName} ${direction == ROTATION_DIRECTION.CLOCKWISE.ID ? "Increasing" : "Decreasing" } volume from ${volume} to ${newVolume} by ${amount}"

    setVolumeLevel(newVolume, type)
}



private Map getBatteryEvent(value) {
    DEBUG "Battery event ${value}"

    def result = [:]
    result.value = value
    result.name = 'battery'
    result.descriptionText = "${device.displayName} battery was ${result.value}%"
    return result
}


private Long getWhenLastButtonHeld() {
  def result = device.currentValue("whenLastButtonHeld")

  DEBUG "Getting when Last button held: ${result}"

  return result.toLong() as Long
}

private Map getButtonEvent(Map descMap) {
    def buttonState = ""
    def buttonNumber = 0
    def amount = 0;
    def type = EVENT_TYPE.ANALOGUE
    Map result = [:]
	DEBUG "  Processing Button Event: Cluster = ${descMap.clusterInt} command: ${descMap.command}"


    if (descMap.clusterInt == DEVICE_CLUSTERS.DeviceCustom && descMap.command  == DEVICE_CLUSTER_COMMANDS.DeviceCustom.Push) {

        buttonState = BUTTON_STATES.PUSHED
        buttonNumber = descMap.data[1] as Integer

    } else if (descMap.clusterInt == DEVICE_CLUSTERS.DeviceCustom && descMap.command == DEVICE_CLUSTER_COMMANDS.DeviceCustom.Hold) {

        buttonState = BUTTON_STATES.HELD
        def button = getButtonByDataId(hexStringToInt(descMap.data[0]))
        buttonNumber = button.ID

    } else if (descMap.clusterInt == DEVICE_CLUSTERS.DeviceCustom && descMap.command == DEVICE_CLUSTER_COMMANDS.DeviceCustom.Rotate) {

        amount = hexStringToInt(descMap.value);

        buttonState = BUTTON_STATES.ROTATED

        if (amount <= 0x7FFF) {
            buttonNumber = DEVICE_BUTTONS.ROTATE_CW.ID
        } else {
            buttonNumber = DEVICE_BUTTONS.ROTATE_ACW.ID
        }

    }

    if (buttonState == BUTTON_STATES.PUSHED || buttonState == BUTTON_STATES.HELD) {

        sendButtonEvent(buttonNumber, buttonState, type)

    } else if (buttonState == BUTTON_STATES.ROTATED) {

        triggerRotationEvent(buttonNumber, buttonState, amount, type)

    }

    result
}

private sendButtonEvent(buttonNumber, buttonState, type) {
    DEBUG("Button STATE: ${buttonState}  NUMBER: ${buttonNumber}")

    def descriptionText = "$device.displayName button ${buttonNumber} (${getButtonById(buttonNumber).NAME}) was pushed"

	if (buttonNumber > 0) {

		sendEvent(name: "pushed", value: buttonNumber, type: type, descriptionText: descriptionText, isStateChange: true)
		
		INFO("sendButtonEvent: sendEvent(name: \"pushed\", value: ${buttonNumber}, descriptionText: \"${descriptionText}\", isStateChange: true)")
    }

    descriptionText = "$device.displayName a button was pushed"

    sendEvent(name: "whenLastButtonPushed", value: now(), type: type, descriptionText: descriptionText, isStateChange: true)



}

private triggerRotationEvent(buttonNumber, buttonState, amount, type) {
    DEBUG("triggerRotationEvent() triggered: ${buttonState}  buttonId: ${buttonNumber} amount: ${amount}")

	if (buttonNumber == DEVICE_BUTTONS.ROTATE_CW.ID || buttonNumber == DEVICE_BUTTONS.ROTATE_ACW.ID) {

        def direction = buttonNumber == DEVICE_BUTTONS.ROTATE_CW.ID ? ROTATION_DIRECTION.CLOCKWISE.ID : ROTATION_DIRECTION.ANTI_CLOCKWISE.ID

        def calculatedLevel = 0;

        if (type == EVENT_TYPE.ANALOGUE && buttonNumber == DEVICE_BUTTONS.ROTATE_ACW.ID) {

            // Anti-clockwise rotation will start at 65535 (0xFFFF) and count backwards, so we normalise this to make
            // clockwise count up from zero (normal) and anti-clockwise to count down from zero (going in to the negatives)
            def newAmount = -(amount - 65535 - 1);

            DEBUG("Normalising input from analogue button rotating anti-clockwise converting ${amount} to ${newAmount}")

            amount = newAmount

        }


        // The natural increment of the dial (the small possible unit of change0 is 12
        // when you turn the dial you may experience a slight tactile sensation like a
        // bumping sesation, each bump represents 12 degress of rotation
        def divisions = amount / 12 as Integer

        setLevelBy(divisions, direction, type)
        moveVolumeBy(divisions, direction, type)
        setRotationAmountBy(divisions, direction, type)

        sendEvent(name: "rotationDirection", value: getRotationById(direction).STRING_ID, type: type, descriptionText: "Dial was rotated %{direction}", isStateChange: true)

        INFO("Rotation event completed: buttonId: ${buttonNumber} buttonState: ${buttonState}, inputAmount: ${amount}, calculatedLevel: ${calculatedLevel}, levelUnitOfMeasure: ${levelUnitOfMeasure}, volumeDivisions: ${divisions} type: ${type}")
	}
}

private Integer getLevel() {
    def levelPreset = device.currentValue("levelPreset")

    DEBUG "Getting level ${levelPreset}"

    return levelPreset as Integer
}

private void setLevelBy(amount, direction, type) {
    def descriptionText = "$device.displayName triggered level change value: ${amount}${levelUnitOfMeasure}"
    def level = getLevel()


    if (isLevelPresetChangeRelative == "yes") {
        amount = level + amount
    } else {
        amount = amount
    }


    DEBUG("setLevelBy triggered amount: ${amount}, which is roughly ${caclulatedAmount} levelUnitOfMeasure: ${levelUnitOfMeasure}, type: ${type}")

    if (amount < 0) {
        amount = 0
    }
    if (amount > 100) {
        amount = 100
    }

	sendEvent(name: "levelPreset", value: amount, unit: levelUnitOfMeasure, type: type, descriptionText: descriptionText, isStateChange: true)
}

private getRotationAmount() {
    def currentRotationAmount = device.currentValue("rotationAmount")
    //currentRotationAmount = currentRotationAmount ? currentRotationAmount : 0;

    DEBUG "Getting rotation amount ${currentRotationAmount}"

    return currentRotationAmount as Integer
}

/**
Rotation amount is similar to level except that it is to restricted to the range 0-100 and
has the option of providing a value that is relative to the last value or the absolute value
of the knob

*/
private setRotationAmountBy(amount, direction, type) {
    def previousValue = getRotationAmount()
    def descriptionText = "setRotationAmountBy() triggered amount: ${amount} direction: ${direction}, type: ${type}, isRotationChangeRelative: ${isRotationChangeRelative}"
    def translatedAmount = 0;
    def newValue = 0;



    if (rotationUnitOfMeasure == LEVEL_UNIT_OF_MEASURE.DIVISIONS.UNIT) {

        translatedAmount = amount

    } else if (rotationUnitOfMeasure == LEVEL_UNIT_OF_MEASURE.PERCENT.UNIT) {

        // The natural increment is 12, there are 30 divisions around the knob, which naturally means a whole turn is 30 x 12 = 360°
        // divide the amount by 12, then the result by 30 to get a percentage of a turn.
        translatedAmount = (amount / 30) * 100  as Integer

    } else if (rotationUnitOfMeasure == LEVEL_UNIT_OF_MEASURE.DEGREES.UNIT) {

        rotationUnitOfMeasure = LEVEL_UNIT_OF_MEASURE.DEGREES.UNIT
        // each division is 12°
        translatedAmount = amount * 12

    } else {

        ERROR("Unsupported unit of measure for rotation: ${rotationUnitOfMeasure} ${LEVEL_UNIT_OF_MEASURE.DEGREES.UNIT} ${LEVEL_UNIT_OF_MEASURE.DEGREES.UNIT == rotationUnitOfMeasure}")
        return
    }

    if (direction == ROTATION_DIRECTION.ANTI_CLOCKWISE.ID) {

        translatedAmount = -translatedAmount

    }

    if (isRotationChangeRelative == "yes") {
        newValue = previousValue + translatedAmount

    } else {
        newValue = translatedAmount
    }

    newValue = newValue * rotationMultiplier


    DEBUG("setRotationAmountBy triggered previous value was ${previousValue}, newValue: ${newValue}, isRotationChangeRelative: ${isRotationChangeRelative}, translatedAmount: ${translatedAmount}, rotationUnitOfMeasure: ${rotationUnitOfMeasure}")

    sendEvent(name: "rotationAmount", value: newValue, unit: rotationUnitOfMeasure, type: type, descriptionText: descriptionText, isStateChange: true)

}

private hexStringToInt(hexString) {
    return hubitat.helper.HexUtils.hexStringToInt(hexString)
}



private List addHubToGroup(Integer groupAddr) {
    ["he cmd 0x0000 0x01 ${CLUSTER_GROUPS} 0x00 {${zigbee.swapEndianHex(zigbee.convertToHexString(groupAddr,4))} 00}", "delay 200"]
}

private List readDeviceBindingTable() {
    INFO("readDeviceBindingTable called..." + ["zdo mgmt-bind 0x${device.deviceNetworkId} 0", "delay 200"] + ["zdo active 0x${device.deviceNetworkId}"])
}

