/*
 * Licensed Materials - Property of IBM
 * 5725-I43 (C) Copyright IBM Corp. 2013. All Rights Reserved.
 * US Government Users Restricted Rights - Use, duplication or
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */
//
//  WLWebFrameworkInitResult.h
//  WorklightStaticLibProject
//
//

#import <Foundation/Foundation.h>

/**
 *
 * An enumeration of IBM MobileFirst Platform web framework initialization status codes.
 */
 typedef NS_ENUM(NSInteger, StatusCode) {
    /** Successful initialization. Web resources are ready to be used */
    WLWebFrameworkInitResultSuccess, 
    /** Initialization failed because the checksum check was unsuccessful. */
    WLWebFrameworkInitResultFailureCheksum,
    /** Initialization failed because web resources extraction was unsuccessful. */
    WLWebFrameworkInitResultFailureUnzip,
    /** Initialization failed because there is not enough space on the device. */
    WLWebFrameworkInitResultFailureNotEnoughSpace,
    /** Initialization failed due to an internal error. */
    WLWebFrameworkInitResultFailureInternal
};
 
//typedef enum {
//    WLWebFrameworkInitResultSuccess, /** Successful initialization. Web resources are ready to be used */
//    WLWebFrameworkInitResultFailureCheksum, /** Initialization failed because the checksum check was unsuccessful. */
//    WLWebFrameworkInitResultFailureUnzip, /** Initialization failed because web resources extraction was unsuccessful. */
//    WLWebFrameworkInitResultFailureNotEnoughSpace, /** Initialization failed because there is not enough space on the device. */
//    WLWebFrameworkInitResultFailureInternal /** Initialization failed due to an internal error. */
//} StatusCode;

/**
 *
 * The result object containing information about the IBM MobileFirst Platform web framework initialization status.
 *.
 */
@interface WLWebFrameworkInitResult : NSObject

- (id)initWithStatusCode:(StatusCode)statusCode message:(NSString *)message data:(NSDictionary*) data;


//properties
/**
 * The IBM MobileFirst Platform web framework initialization status code.
 */
@property (nonatomic, assign, readonly) StatusCode statusCode;

/** 
 * The IBM MobileFirst Platform web framework initialization message.
 */
@property (nonatomic, copy, readonly)   NSString* message;

/**
 * Additional web framework initialization data, e.g. the amount of required space on the device.
 *
 * When the StatusCode WLWebFrameworkInitResultFailureNotEnoughSpace is returned, accessing the data dictionary with the string "spaceRequired" as key returns the amount of required space, in bytes.
 */
@property (nonatomic, strong, readonly) NSDictionary* data;

@end
