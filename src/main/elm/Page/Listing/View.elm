module Page.Listing.View exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Markdown

import Data.Script exposing (Script)
import Page.Listing.Data exposing (..)

view: Model -> Html Msg
view model =
    div [class "listing-page"]
        [div [class "ui segments"]
             (List.map viewScript model.list)
        ]

viewScript: Script -> Html Msg
viewScript script =
    div [class "ui segment"]
        [h4  [ class "ui header" ]
             [ text script.name ]
        ,div []
            [Markdown.toHtml [class "content"] script.description
            ]
        ]
