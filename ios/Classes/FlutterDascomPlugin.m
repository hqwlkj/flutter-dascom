#import "FlutterDascomPlugin.h"
#if __has_include(<flutter_dascom/flutter_dascom-Swift.h>)
#import <flutter_dascom/flutter_dascom-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "flutter_dascom-Swift.h"
#endif

@implementation FlutterDascomPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterDascomPlugin registerWithRegistrar:registrar];
}
@end
