//
//  MyAppDelegate.h
//
//

#import <IBMMobileFirstPlatformFoundationHybrid/IBMMobileFirstPlatformFoundationHybrid.h>

@class Reachability;

@interface MyAppDelegate : WLAppDelegate <WLInitWebFrameworkDelegate> {
    
}

@property (nonatomic) BOOL isNetworkConnected;
@property (nonatomic, strong) Reachability* internetReach;
@property (nonatomic, strong) CDVViewController *cordovaViewController;
@property (atomic, strong) NSString* clientId;
@property (atomic, strong) NSString* topic;

@end
