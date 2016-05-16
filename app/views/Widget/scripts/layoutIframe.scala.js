@(
  widgetId:String,
  widgetConfig:play.api.libs.json.JsObject,
  env:play.api.Environment
)(content: JavaScript)
@includeScript(filePath:String) = @{

    env.resourceAsStream(filePath) match {
        case Some(is) => scala.io.Source.fromInputStream(is).getLines().mkString("\n")
        case _ => ""
    }
}
/* IBS  Business chat v-1 */
@JavaScript( includeScript("public/javascripts/ui/jquery-1.11.js") )


( function( $jq, widgetId, widgetConfig ){

    @JavaScript( includeScript("public/javascripts/ui/toastr.min.js") )
    @JavaScript( includeScript("public/javascripts/ui/jquery.ba-postmessage.min.js") )

    var widgetHtml = "@JavaScriptFormat.escape( views.html.Widget.templateIframe( widgetId, widgetConfig ).toString )";

    // widget
    @content

} )( jQuery.noConflict(true), "@widgetId", @JavaScript(widgetConfig.toString) );
