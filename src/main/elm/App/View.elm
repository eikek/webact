module App.View exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)

import App.Data exposing (..)
import Page.Listing.View

view: Model -> Html Msg
view model =
    div [class "default-layout"]
        [ div [class "ui fixed top sticky attached large menu black-bg"]
              [div [class "ui fluid container"]
                   [ a [class "header item narrow-item"]
                         [i [classList [("large cog icon", True)
                                       ]]
                              []
                         ,text "Webact"]
                   ]
              ]
        , div [ class "ui container main-content" ]
            [ (Html.map ListingMsg (Page.Listing.View.view model.listingModel))
            ]
        ]
