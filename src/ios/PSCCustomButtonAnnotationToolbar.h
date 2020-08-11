#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import <PSPDFKit/PSPDFKit.h>
#import <PSPDFKitUI/PSPDFKitUI.h>
#import "PSPDFKitPlugin.h"

@interface PSCCustomButtonAnnotationToolbar : PSPDFAnnotationToolbar

@property (nonatomic) PSPDFToolbarButton *editAssetButton;
+ (void)setPluginReference:(PSPDFKitPlugin *)pluginReference;

@end