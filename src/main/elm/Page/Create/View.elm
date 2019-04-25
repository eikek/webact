module Page.Create.View exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onInput, onClick)

import Page exposing (Page(..))
import Page.Create.Data exposing (..)

view: Model -> Html Msg
view model =
    div []
        [ h1 [class "ui dividing header"]
             [text "Create new Script"
             ]
        ,div [class "ui warning form"]
            [ div [classList [("field", True)
                             ,("error", not model.nameValid)]
                  ]
                  [label [][text "Name*"]
                  ,input [type_ "text"
                         ,onInput SetName
                         ,value model.name
                         ][]
                  ,div [classList [("ui warning message", True)
                                  ,("invisible", not model.nameExists)
                                  ]
                       ]
                      [ text "This name already exists! You will overwrite this existing script on Save!"
                      ]
                  ]
            , div [classList [("field", True)
                             ,("error", not model.contentValid)
                             ]
                  ]
                  [label [][text "Content*"]
                  ,textarea [rows 21
                            ,onInput SetContent
                            ][text model.content]
                  ]
            , div [class ""]
                [button [class "ui primary button"
                        ,onClick SaveScript
                        ,Page.href CreatePage
                        ]
                     [text "Save"
                     ]
                ,a [classList [("ui button", True)
                                   ,("invisible", model.saved /= (Just True))]
                        ,Page.href (DetailPage model.name)
                        ]
                    [text "Goto Script"
                    ]
                ,span [classList [("invisible", model.saved /= (Just True))
                                 ,("ui green header", True)]
                      ]
                    [text "Successfully saved."
                    ]
                ,span [classList [("invisible", model.saved /= (Just False))
                                 ,("ui red header", True)
                                 ]
                      ]
                     [text "Saving the script failed!"
                     ]
                ]
            ]
        ]
