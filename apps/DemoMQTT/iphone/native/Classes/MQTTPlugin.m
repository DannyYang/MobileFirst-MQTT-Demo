//
//  MQPlugin.m
//  MobileClientMPortalIphone
//
//  Created by baytony on 2013/12/19.
//
//

#import "MQTTPlugin.h"
#import "MQTTConstant.h"
#import "DemoMQTT.h"
#import "MqttOCClient.h"
#import "MQTTMessenger.h"


@implementation MQTTPlugin

//MQTTClientWrapper *mqttClient;
NSTimer *timer;

// clientId & topic
- (void) CONNECT: (CDVInvokedUrlCommand*)command
{
    //取得ＪＳ傳入進來的參數
    if(command.arguments!=nil && command.arguments.count==2){
        self.myAppDelegate = (MyAppDelegate *)[[UIApplication sharedApplication] delegate];
        self.myAppDelegate.clientId = [[command.arguments objectAtIndex:0] copy];
        self.myAppDelegate.topic = [[command.arguments objectAtIndex:1] copy];
        [self startMQTT: command.callbackId];
    }else{
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Connect MQTT Client failed"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

- (void) DISCONNECT: (CDVInvokedUrlCommand*)command
{
    //當切到login畫面的時候,要把MQTT斷線並把暫存清掉,避免在登出狀態時收到MQTT訊息
    self.myAppDelegate = (MyAppDelegate *)[[UIApplication sharedApplication] delegate];
    self.myAppDelegate.clientId = nil;
    self.myAppDelegate.topic = nil;
    [self stopMQTT];
}

- (void) SEND_MSG: (CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    self.myAppDelegate = (MyAppDelegate *)[[UIApplication sharedApplication] delegate];
    if(command.arguments != nil && command.arguments.count == 2 && self.myAppDelegate.isNetworkConnected){
        NSString *topic = [command.arguments objectAtIndex:0];
        NSString *msg = [command.arguments objectAtIndex:1];
        
        if([[MQTTMessenger sharedMessenger] isConnected]){
            [[MQTTMessenger sharedMessenger] publish:topic payload:msg qos:MSG_QOS retained:MSG_RETAINED];
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        }else {
            NSLog(@"MQTT Client is not connected...");
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"publish message failed"];
        }
    }else{
        NSLog(@"-->SEND_MSG MQTT command argument ERROR! The argument count is nil or not 3 or no networkconnection");
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Send MQTT MSG failed"];
    }
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

#pragma mark Start/Stop/ReStart MQTT Connection
//開始建立ＭＱＴＴ連線，如果連線已經存在，就重先連線．
- (void) startMQTT:(NSString *) callbackID
{
    if([[MQTTMessenger sharedMessenger] isConnected]){
        [self stopMQTT];
    }
    self.myAppDelegate = (MyAppDelegate *)[[UIApplication sharedApplication] delegate];
    if(self.myAppDelegate.clientId!=nil && self.myAppDelegate.topic!=nil ){
        NSLog(@"-->Start Connecting to MQTT Server!");
        if(callbackID){
            [NSThread detachNewThreadSelector:@selector(threadEntry:)
                                     toTarget:self
                                   withObject:[NSArray arrayWithObjects: callbackID, nil]];
        }else{
            [NSThread detachNewThreadSelector:@selector(threadEntry:)
                                     toTarget:self
                                   withObject:[NSArray arrayWithObjects: nil]];
        }
    }else{
        NSLog(@"-->No Data found! MQTT Server not connected!");
    }
}
//透過Timer註冊一個重新建立ＭＱＴＴ連線的Thread.
-(void) scheduleRestart
{
    NSLog(@"Schedule restart %d in sec...", MQTT_KEEP_ALIVE);
    dispatch_async(dispatch_get_main_queue(), ^{
        timer = [NSTimer scheduledTimerWithTimeInterval:MQTT_KEEP_ALIVE target:self selector:@selector(restartMQTT:) userInfo:nil repeats:NO];
    });
}
//馬上重先建立ＭＱＴＴ連線
-(void) restartMQTT:(NSTimer *)timer
{
    NSLog(@"-->Restarting MQTT.");
    [self startMQTT:nil];
}

- (void) stopMQTT
{
    NSLog(@"-->Stop Connection to MQTT Server!");
    if([MQTTMessenger sharedMessenger]!=nil && [[MQTTMessenger sharedMessenger] isConnected]) {
        [(MQTTMessenger*)[MQTTMessenger sharedMessenger] disconnectWithTimeout:0];
        NSLog(@"-->mqttClient Disconnected from Server!");
    }
}

- (BOOL)unsubscribe:(NSString *)topic;
{
    //    BOOL success = false;
    self.myAppDelegate = (MyAppDelegate *)[[UIApplication sharedApplication] delegate];
    if(self.myAppDelegate.isNetworkConnected && [[MQTTMessenger sharedMessenger] isConnected]) {
        NSLog(@"unsubscribe topic:%@",topic);
        [(MQTTMessenger*)[MQTTMessenger sharedMessenger] unsubscribe:topic];
    }
    // TODO : return value
    return YES;
}

// run the mqtt client in its own thread
#pragma mark MQTT Thread Method to connect the Server.
- (void)threadEntry:(NSArray *)params {
    @autoreleasepool {
        @synchronized(self){
            self.myAppDelegate = (MyAppDelegate *)[[UIApplication sharedApplication] delegate];
            if(self.myAppDelegate.isNetworkConnected) {
                if([[MQTTMessenger sharedMessenger] isConnected])
                    return;
                
                [self stopMQTT];
                
                NSString *serverAddr = [NSString stringWithFormat: @"%@", MQTT_HOST];
                // 產生 TOPIC 字串
                NSString *topic = @"";
                
                NSString *callbackID = nil;
                if(params.count==1){
                    callbackID = [params objectAtIndex:0];
                }
                
                NSLog(@"-->Start to CONNECT to MQTT server:%@, topic:%@, callbackID:%@",serverAddr,topic,callbackID);
                
                [[MQTTMessenger sharedMessenger] setListener:self];
                
                // create the mqtt client
                NSArray *brokerAddressArray = [NSArray arrayWithObject:serverAddr];
                NSArray *brokerPortArray = [NSArray arrayWithObject:MQTT_PORT];
                
                ConnectOptions *opts = [[ConnectOptions alloc] init];
                opts.timeout = 5;
                opts.keepAliveInterval = MQTT_KEEP_TIME_INTERVAL;
                opts.cleanSession = CLEAR_SESSION;
                
                [[MQTTMessenger sharedMessenger] connectWithHosts:brokerAddressArray ports:brokerPortArray clientId:[NSString stringWithFormat:@"%@",self.myAppDelegate.clientId] connectOptions:opts];
            } else {
                NSLog(@"Network unavailable, can't connect MQServer.");
                [self scheduleRestart];
            }
        } // end of synchronized
    } // end of autoreleasepool
}

#pragma mark MQTT Callback Handler
/**
 * Method called when a connection is unexpectedly lost
 * The callback must be performed on the main thread because you can't make a uikit call (uiwebview) from another thread
 */
- (void)connectionLost
{
    NSLog(@"Connection to broker lost! Ready to reconnect in %d sec...", MQTT_KEEP_ALIVE);
    
    if([MQTTMessenger sharedMessenger]!=nil) {
        [[MQTTMessenger sharedMessenger] disconnectWithTimeout:0];
    }
    dispatch_sync(dispatch_get_main_queue(), ^{
        [self scheduleRestart];
    });
}

- (void) callJsDelegate:(NSString *)js {
    [self.commandDelegate evalJs:js];
}

// 使用ARC無需管理記憶體釋放，編譯器將會自動補上

@end
