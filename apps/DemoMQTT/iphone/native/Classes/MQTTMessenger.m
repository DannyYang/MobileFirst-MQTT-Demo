//
//  Messenger.m
//  MQTTTest
//
//  Created by DannyYang
//

#import <stdio.h>
#import <signal.h>
#import <memory.h>
#import <sys/time.h>
#import <stdlib.h>
#import <zlib.h>

#import "MQTTConstant.h"
#import "MQTTMessenger.h"
#import "MqttOCClient.h"
#import "DemoMQTT.h"

// Connect Callbacks
@interface ConnectCallbacks : NSObject <InvocationComplete>
- (void) onSuccess:(NSObject*) invocationContext;
- (void) onFailure:(NSObject*) invocationContext errorCode:(int) errorCode errorMessage:(NSString*) errorMessage;
@end

@implementation ConnectCallbacks
- (void) onSuccess:(NSObject*) invocationContext
{
    NSLog(@"%s:%d - invocationContext=%@", __func__, __LINE__, invocationContext);
    [NSThread detachNewThreadSelector:@selector(subscribeDomainServer)
                             toTarget:self
                           withObject:nil];
}

- (void) onFailure:(NSObject*) invocationContext errorCode:(int) errorCode errorMessage:(NSString*) errorMessage
{
    [[[MQTTMessenger sharedMessenger] listener] scheduleRestart];
    NSLog(@"%s:%d - invocationContext=%@  errorCode=%d  errorMessage=%@", __func__,
        __LINE__, invocationContext, errorCode, errorMessage);

}

- (void)subscribeDomainServer{
    NSLog(@"[subscribeDomainServer]");
    @autoreleasepool {
        @synchronized(self){
            MyAppDelegate *myAppDelegate = (MyAppDelegate *)[[UIApplication sharedApplication] delegate];
            if(myAppDelegate.isNetworkConnected && [[MQTTMessenger sharedMessenger] isConnected]) {
                NSString *topic = myAppDelegate.topic;
                [[MQTTMessenger sharedMessenger] subscribe:topic qos:MSG_QOS];
            } else {
                NSLog(@"Try to subscribe domain server, but the connection is lose.");
            }
        }
    }
}
@end

// Publish Callbacks
@interface PublishCallbacks : NSObject <InvocationComplete>
- (void) onSuccess:(NSObject*) invocationContext;
- (void) onFailure:(NSObject*) invocationContext errorCode:(int) errorCode errorMessage:(NSString *)errorMessage;
@end
@implementation PublishCallbacks
- (void) onSuccess:(NSObject *) invocationContext
{
    NSLog(@"PublishCallbacks - onSuccess");
}
- (void) onFailure:(NSObject *) invocationContext errorCode:(int) errorCode errorMessage:(NSString *)errorMessage
{
    NSLog(@"PublishCallbacks - onFailure");
}
@end

// Subscribe Callbacks
@interface SubscribeCallbacks : NSObject <InvocationComplete>
- (void) onSuccess:(NSObject*) invocationContext;
- (void) onFailure:(NSObject*) invocationContext errorCode:(int) errorCode errorMessage:(NSString*) errorMessage;
@end
@implementation SubscribeCallbacks
- (void) onSuccess:(NSObject*) invocationContext
{
    NSLog(@"SubscribeCallbacks - onSuccess");
}
- (void) onFailure:(NSObject*) invocationContext errorCode:(int) errorCode errorMessage:(NSString*) errorMessage
{
    NSLog(@"SubscribeCallbacks - onFailure");
}
@end

// Unsubscribe Callbacks
@interface UnsubscribeCallbacks : NSObject <InvocationComplete>
- (void) onSuccess:(NSObject*) invocationContext;
- (void) onFailure:(NSObject*) invocationContext errorCode:(int) errorCode errorMessage:(NSString*) errorMessage;
@end
@implementation UnsubscribeCallbacks
- (void) onSuccess:(NSObject*) invocationContext
{
    NSLog(@"UnsubscribeCallbacks - onSuccess:%d - invocationContext=%@", __LINE__, invocationContext);
}
- (void) onFailure:(NSObject*) invocationContext errorCode:(int) errorCode errorMessage:(NSString*) errorMessage
{
    NSLog(@"UnsubscribeCallbacks - onFailure:%d - invocationContext=%@  errorCode=%d  errorMessage=%@", __LINE__, invocationContext, errorCode, errorMessage);
}
@end

@interface GeneralCallbacks : NSObject <MqttCallbacks>
- (void) onConnectionLost:(NSObject*)invocationContext errorMessage:(NSString*)errorMessage;
- (void) onMessageArrived:(NSObject*)invocationContext message:(MqttMessage*)msg;
- (void) onMessageDelivered:(NSObject*)invocationContext messageId:(int)msgId;
@end
@implementation GeneralCallbacks
- (void) onConnectionLost:(NSObject*)invocationContext errorMessage:(NSString*)errorMessage
{
    NSLog(@"[MQTT Client][onConnectionLost] errorMessage:%@",errorMessage);
    [[[MQTTMessenger sharedMessenger] listener] connectionLost];
}
- (void) onMessageArrived:(NSObject*)invocationContext message:(MqttMessage*)msg
{
    NSLog(@"GeneralCallbacks - onMessageArrived!");
    NSLog(@"msg : %s",msg.payload);
    NSString *jsCallStr = [NSString stringWithFormat:@"showPushMsg('%@');",[[NSString alloc]initWithCString:msg.payload encoding:NSUTF8StringEncoding]];
    [[[MQTTMessenger sharedMessenger] listener] callJsDelegate:jsCallStr];
}

- (void) onMessageDelivered:(NSObject*)invocationContext messageId:(int)msgId
{
    NSLog(@"GeneralCallbacks - onMessageDelivered!");
}
@end


@implementation MQTTMessenger

@synthesize client;
@synthesize regCommand;

#pragma mark Singleton Methods

+ (id)sharedMessenger {
    static MQTTMessenger *shared = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        shared = [[self alloc] init];
    });
    return shared;
}

- (id)init {
    if (self = [super init]) {
        self.client = [MqttClient alloc];
        self.clientID = nil;
        self.client.callbacks = [[GeneralCallbacks alloc] init];
    }
    return self;
}

- (void)connectWithHosts:(NSArray *)hosts ports:(NSArray *)ports clientId:(NSString *)clientId connectOptions:(ConnectOptions*)opts
{
    client = [client initWithHosts:hosts ports:ports clientId:clientId];
    [client connectWithOptions:opts invocationContext:self onCompletion:[[ConnectCallbacks alloc] init]];
}

- (void)disconnectWithTimeout:(int)timeout {
    DisconnectOptions *opts = [[DisconnectOptions alloc] init];
    [opts setTimeout:timeout];
    
    [client disconnectWithOptions:opts invocationContext:self onCompletion:[[ConnectCallbacks alloc] init]];
}

- (void)publish:(NSString *)topic payload:(NSString *)payload qos:(int)qos retained:(BOOL)retained
{
    char* sendData = (char*)[payload UTF8String];
    int sendDataLen = (int)strlen(sendData);
    
    MqttMessage *msg = [[MqttMessage alloc] initWithMqttMessage:topic payload:sendData length:sendDataLen qos:qos retained:retained duplicate:NO];
    [client send:msg invocationContext:self onCompletion:[[PublishCallbacks alloc] init]];
}

- (void)subscribe:(NSString *)topicFilter qos:(int)qos
{
    NSLog(@"%s:%d topicFilter=%@, qos=%d", __func__, __LINE__, topicFilter, qos);
    [client subscribe:topicFilter qos:qos invocationContext:topicFilter onCompletion:[[SubscribeCallbacks alloc] init]];
}

- (void)unsubscribe:(NSString *)topicFilter
{
    NSLog(@"%s:%d topicFilter=%@", __func__, __LINE__, topicFilter);
    [client unsubscribe:topicFilter invocationContext:topicFilter onCompletion:[[UnsubscribeCallbacks alloc] init]];
}

- (BOOL)isConnected
{
    return [client isConnected];
}
@end