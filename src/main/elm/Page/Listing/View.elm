module Page.Listing.View exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick)
import Markdown

import Util.List
import Data.Script exposing (Script)
import Data.ScriptInfo exposing (ScriptInfo)
import Page exposing (Page(..))
import Page.Listing.Data exposing (..)

view: Model -> Html Msg
view model =
    div [class "listing-page"]
        [h1 [class "ui dividing header"][text "Actions"]
        ,(headMenu model)
        ,table [class "ui very basic selectable table"]
            [ thead []
                  [
                  ]
            , List.filter (categoryFilter model) model.scripts
              |> List.map scriptItemRow
              |> tbody []
            ]
        ]

scriptItemRow: ScriptInfo -> Html Msg
scriptItemRow info =
    tr []
       [ td [ class "collapsing" ]
            <| scriptIcons info

       , td [ class "collapsing" ]
            [ h3 [ class "ui header" ]
                 [ a [ Page.href (DetailPage info.script.name) ] [ text info.script.name ]
                 ]
            ]
       , td []
            <| scriptSummary info
       , td [ class "right aligned collapsing" ]
            (List.map (\c -> span[class "ui tag label"][text c]) info.script.category)
       ]

scriptIcons: ScriptInfo -> List (Html Msg)
scriptIcons info =
    let
        success = Maybe.map .success info.output |> Maybe.withDefault True
    in
    [ i [classList [("large orange loading cog icon", True)
                   ,("invisible", info.script.executing == 0)
                   ]
        ][]
    , i [classList [("large green check icon", True)
                   ,("invisible", not (info.script.enabled && success))]
        ][]
    , i [classList [("large red bolt icon", True)
                   ,("invisible", not (info.script.enabled && (not success)))]
        ][]
    , i [classList [("large disabled red ban icon", True)
                   ,("invisible", info.script.enabled)]
        ][]
    , i [classList [("large calendar check outline icon", True)
                   ,("invisible", info.script.schedule == "")]
        ][]
    ]

scriptSummary: ScriptInfo -> List (Html Msg)
scriptSummary info =
      [ span [classList [("ui blue label", True)
                        ,("invisible", info.script.schedule == "")]]
            [ i [class "calendar icon"][]
            , text info.script.schedule
            ]
      , span [classList [("ui basic blue label", True)
                        ,("invisible", info.script.notifyMail == "")]]
          [ i [class "envelope outline icon"][]
          , text info.script.notifyMail
          ]
      , span [classList [("ui basic orange label", True)
                        ,("invisible", info.script.notifyErrorMail == "")]]
          [ i [class "red envelope icon"][]
          , text info.script.notifyErrorMail
          ]
      , span [class "ui label"]
          [ i [class "small pencil alternate icon"][]
          , Data.Script.lastMod info.script |> text
          ]
      ]

headMenu: Model -> Html Msg
headMenu model =
    let
        cats = ["All"] ++
               (List.map .script model.scripts |>
                    List.concatMap .category |>
                    Util.List.distinct |>
                    List.sort)

        makeLink: String -> Html Msg
        makeLink cat =
            a [ classList [("active", cat == model.category)
                          ,("item", True)
                          ]
              , Page.href ListingPage
              , onClick (SetCategory cat)
              ]
            [ text cat ]

        addButton: Html Msg
        addButton =
            div [ class "right menu" ]
                [ a [class "item"
                    ,Page.href CreatePage
                    ]
                      [ i [class "add icon"] []
                      , text "Add"
                      ]
                ]
    in
    div [class "ui pointing menu"]
        ((List.map makeLink cats) ++ [addButton])

categoryFilter: Model -> ScriptInfo -> Bool
categoryFilter model info =
    model.category == "All" || List.member model.category info.script.category
