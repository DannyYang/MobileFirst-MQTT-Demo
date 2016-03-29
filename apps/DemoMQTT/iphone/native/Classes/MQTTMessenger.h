//
//  Messenger.h
//  MQTTTest
//
//  Created by DannyYang
//

#import <Foundation/Foundation.h>
#import "MqttOCClient.h"
#import "MQTTListener.h"
#import "MQTTPlugin.h"

@interface MQTTMessenger : NSObject {
    MqttClient *client;
}
@property (nonatomic, strong) NSString* regCommand;
@property (nonatomic, strong) MqttClient *client;
@property (nonatomic, strong) NSString *clientID;
@property (assign) id<MQTTListener> listener;

+ (id)sharedMessenger;
- (void)connectWithHosts:(NSArray *)hosts ports:(NSArray *)ports clientId:(NSString *)clientId connectOptions:(ConnectOptions*)opts;
- (void)publish:(NSString *)topic payload:(NSString *)payload qos:(int)qos retained:(BOOL)retained;
- (void)subscribe:(NSString *)topicFilter qos:(int)qos;
- (void)unsubscribe:(NSString *)topicFilter;
- (void)disconnectWithTimeout:(int)timeout;
- (BOOL)isConnected;

@end
