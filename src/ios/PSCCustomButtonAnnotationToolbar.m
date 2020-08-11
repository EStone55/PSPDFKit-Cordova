#import "PSCCustomButtonAnnotationToolbar.h"
#import "PSPDFKitPlugin.h"

@implementation PSCCustomButtonAnnotationToolbar

static PSPDFKitPlugin *_pluginReference = nil;

- (instancetype)initWithAnnotationStateManager:(PSPDFAnnotationStateManager *)annotationStateManager {
    if ((self = [super initWithAnnotationStateManager:annotationStateManager])) {
        UIImage *editImage = [[PSPDFKitGlobal imageNamed:@"trash"] imageWithRenderingMode:UIImageRenderingModeAlwaysTemplate];
        _editAssetButton = [PSPDFToolbarButton new];
        _editAssetButton.accessibilityLabel = @"Edit Asset";
        [_editAssetButton setImage:editImage];
        [_editAssetButton addTarget:self action:@selector(editAssetButtonPressed:) forControlEvents:UIControlEventTouchUpInside];

        self.additionalButtons = @[_editAssetButton];
    }
    return self;
}

+ (void)setPluginReference:(PSPDFKitPlugin *)pluginReference {
    if (pluginReference != _pluginReference) {
        _pluginReference = pluginReference;
    } 
}

- (void)editAssetButtonPressed:(id)sender {
    PSPDFViewController *pdfController = self.annotationStateManager.pdfController;
    PSPDFPageView *view = [pdfController pageViewForPageAtIndex:0];
    NSArray<PSPDFAnnotation *> *annotations = view.selectedAnnotations;
    NSArray <NSDictionary *> *annotationsJSON = [PSPDFKitPlugin instantJSONFromAnnotations:annotations];
    if (annotationsJSON) {
        [_pluginReference sendEventWithJSON:@{@"type": @"onOpenAssetActionModal", @"annotations": annotationsJSON}];
    }
}

@end