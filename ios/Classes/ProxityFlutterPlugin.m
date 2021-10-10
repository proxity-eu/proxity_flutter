#import "ProxityFlutterPlugin.h"
#if __has_include(<proxity_flutter/proxity_flutter-Swift.h>)
#import <proxity_flutter/proxity_flutter-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "proxity_flutter-Swift.h"
#endif

@implementation ProxityFlutterPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftProxityFlutterPlugin registerWithRegistrar:registrar];
}
@end
