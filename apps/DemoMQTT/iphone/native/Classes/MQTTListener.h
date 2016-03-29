//
//  MQTTListener.h
//  UIF
//
//  Created by DannyYang
//


@protocol MQTTListener

- (void)callJsDelegate:(NSString *)js;
- (void)connectionLost;
- (void)scheduleRestart;

@optional
- (void)keepAll:(NSString *)appId msg:(NSString *)msg;
- (void)keepLast:(NSString *)appId msg:(NSString *)msg;
@end
