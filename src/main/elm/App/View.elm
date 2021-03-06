module App.View exposing (view)

import App.Data exposing (..)
import Html exposing (..)
import Html.Attributes exposing (..)
import Page exposing (Page(..))
import Page.Create.View
import Page.Detail.View
import Page.Listing.View


view : Model -> Html Msg
view model =
    div [ class "default-layout" ]
        [ div [ class "ui fixed top sticky attached large menu black-bg" ]
            [ div [ class "ui fluid container" ]
                [ a [ class "header item narrow-item" ]
                    [ i
                        [ classList
                            [ ( "cog icon", True )
                            ]
                        ]
                        []
                    , text model.flags.appName
                    ]
                ]
            ]
        , div [ class "ui container main-content" ]
            [ case model.page of
                ListingPage ->
                    viewListing model

                DetailPage _ ->
                    viewDetail model

                CreatePage ->
                    viewCreate model
            ]
        , div [ class "ui footer" ]
            [ a [ href "https://github.com/eikek/webact" ]
                [ i [ class "ui github icon" ] []
                ]
            , span []
                [ text "Webact "
                , text model.version.version
                , text " (#"
                , String.left 8 model.version.gitCommit |> text
                , text ")"
                ]
            ]
        ]


viewListing : Model -> Html Msg
viewListing model =
    Html.map ListingMsg (Page.Listing.View.view model.listingModel)


viewDetail : Model -> Html Msg
viewDetail model =
    Html.map DetailMsg (Page.Detail.View.view model.detailModel)


viewCreate : Model -> Html Msg
viewCreate model =
    Html.map CreateMsg (Page.Create.View.view model.createModel)
