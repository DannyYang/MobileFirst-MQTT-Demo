//
//  MQPlugin.h
//  MobileClientMPortalIphone
//
//  Created by DannyYang


#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>

//#import <Cordova/JSONKit.h>
//#import "MQTTClientWrapper.h"
#import "MQTTListener.h"

@class MyAppDelegate;

@interface MQTTPlugin : CDVPlugin <MQTTListener>

@property (nonatomic, readonly, getter = isConnected) BOOL connected;
@property (nonatomic, weak) MyAppDelegate *myAppDelegate;

//Plugin提供對外的服務
//+ (NSString*) uid;
- (void) CONNECT: (CDVInvokedUrlCommand*)command;
- (void) DISCONNECT: (CDVInvokedUrlCommand*)command;
- (void) SEND_MSG: (CDVInvokedUrlCommand*)command;
- (void) startMQTT:(NSString *) callbackID;
- (void) stopMQTT;
- (void) unsubscribeTopics;
- (void) publishMsgs;

@end
