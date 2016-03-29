
/* JavaScript content from js/main.js in folder common */
function wlCommonInit() {
	/*
	 * Use of WL.Client.connect() API before any connectivity to a Worklight
	 * Server is required. This API should be called only once, before any other
	 * WL.Client methods that communicate with the Worklight Server. Don't
	 * forget to specify and implement onSuccess and onFailure callback
	 * functions for WL.Client.connect(), e.g:
	 * 
	 * WL.Client.connect({ onSuccess: onConnectSuccess, onFailure:
	 * onConnectFailure });
	 * 
	 */
	
	$("#clientID").val(guid());
	
}

/**
 * show message on screen
 * 
 * @param payload
 */
function showPushMsg(payload) {
	navigator.notification.alert('Message:' + payload, // message
	function() {
	}, // callback
	'got msg', // title
	'OK' // buttonName
	);
	console.log(payload);
}

function subscribeTopic() {
	// Subscribe MQTT Topics
	var topic = $("#topicName").val();
	var clientID = $("#clientID").val() + "_" + WL.Client.getEnvironment();

	cordova.exec(function(succ) {
		console.log(JSON.stringify(succ));
	}, function(err) {
		console.log(JSON.stringify(err));
	}, "MQTTPlugin", "CONNECT", [ clientID, topic ]);
}

function testPublishMessage() {
	// publishToTopic MQTT Topics
	var topic = $("#topicName").val();
	var msg = $("#message").val();

	cordova.exec(function(succ) {
		console.log(JSON.stringify(succ));
	}, function(err) {
		console.log(JSON.stringify(err));
	}, "MQTTPlugin", "SEND_MSG", [ topic, msg ]);
}

function guid() {
	function s4() {
		return Math.floor((1 + Math.random()) * 0x10000).toString(16)
				.substring(1);
	}
	return s4() + s4() + '-' + s4();
}
/* JavaScript content from js/main.js in folder android */
// This method is invoked after loading the main HTML and successful initialization of the Worklight runtime.
function wlEnvInit(){
    wlCommonInit();
    // Environment initialization code goes here
}