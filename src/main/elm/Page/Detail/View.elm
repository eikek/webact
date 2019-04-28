module Page.Detail.View exposing (view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onInput)
import Markdown
import File exposing (File)

import Util.Duration
import Util.Size
import Page exposing (Page(..))
import Page.Detail.Data exposing (..)
import Data.Script exposing (Script)
import Data.Output exposing (Output)
import Data.Argument exposing (Argument)
import Data.Param exposing (formatToString, stringToFormat, Format(..))

view: Model -> Html Msg
view model =
    let
        script = Maybe.map .script model.info
        out = Maybe.andThen .output model.info
        lastmod = Maybe.map Data.Script.lastMod script

        prop: a -> (Script -> a) -> a
        prop default getter =
            Maybe.map getter script |> Maybe.withDefault default

        propo: a -> (Output -> a) -> a
        propo default getter =
            Maybe.map getter out |> Maybe.withDefault default

        activeTab: Tab -> Bool
        activeTab name =
            name == model.tab
    in
    div []
        [h1 [class "ui dividing header"]
             [ i [classList [("red ban icon", True)
                            ,("invisible", prop True .enabled)
                            ]][]
             , div [class "content"]
                 [ Maybe.map .name script |> Maybe.withDefault model.scriptName |> text
                 ]
             ]
        , (labels model)
        ,div []
            [prop "" .description |> Markdown.toHtml [class "script-description"]
            ]
        , div [classList
                   [("ui message", True)
                   ,("success", propo False .success)
                   ,("invisible", propo True (.success >> not))
                   ]]
            [text "The last execution was successful."
            ]
        , div [classList
                   [("ui message", True)
                   ,("negative", propo False (.success >> not))
                   ,("invisible", propo True .success)
                   ]]
            [text "The last execution resulted in an error!"
            ]
        , div [classList
                   [("ui warning message", True)
                   ,("invisible", prop False .enabled)
                   ]
              ]
            [text "This script is disabled. It will not be executed!"
            ]
        , div [classList
                   [("ui warning message", True)
                   ,("invisible", (prop 0 .executing) == 0)
                   ]
              ]
            [text "The script is currently executing! Running time "
            ,Util.Duration.toHuman (prop 0 .executing) |> text
            ]
        , div [classList
                   [("ui info message", True)
                   ,("invisible", (prop "" .scheduledAt) == "" || (not <| prop False .enabled))
                   ]
              ]
            [text "The script is scheduled to run next at "
            ,code [][prop "" .scheduledAt |> text]
            ,text "."
            ]
        , (renderTabs model)
        ]

renderTabs: Model -> Html Msg
renderTabs model =
    let
        tabs = List.map (tabContent model) [Run, Content, Stdout, Stderr]
    in
        div []
            ([ div [class "ui top attached tabular menu"]
                  (List.map Tuple.first tabs)
            ] ++ (List.map Tuple.second tabs)
            )


tabContent: Model -> Tab -> (Html Msg, Html Msg)
tabContent model tab =
    let
        activeTab: Tab -> Bool
        activeTab name =
            name == model.tab
    in
        ( a [classList [("item", True)
                       , ("active", activeTab tab)
                       ]
            , Page.href (DetailPage model.scriptName)
            , onClick (ChangeTab tab)
            ]
            [tabToString tab |> text
            ]
        , div [classList
                   [("ui bottom attached tab segment", True)
                   ,("active", activeTab tab)
                   ]
              ]
              [(case tab of
                    Run -> runForm model
                    Content ->  content model
                    Stdout -> stdout model
                    Stderr -> stderr model
               )
              ]
        )


runForm: Model -> Html Msg
runForm model =
    let
        executing = Maybe.map .script model.info
                    |> Maybe.map .executing
                    |> Maybe.withDefault 0
    in
    div []
        [div [class "ui secondary menu"]
             [ div [class "item"]
                   [a [classList [("ui primary button", True)
                                 ,("disabled", executing /= 0)
                                 ]
                      ,Page.href (DetailPage model.scriptName)
                      ,onClick RunScript
                      ]
                      [ text "Run"
                      ]
                   ]
             , div [class "right menu"]
                 [ a [class "item"
                     ,Page.href (DetailPage model.scriptName)
                     ,onClick AddArgument
                     ]
                     [text "Add Argument"
                     ]
                 ]
             ]
        ,div [class "ui form"]
            (List.map (runFormElement model) model.runArgs)
        ]

runFormElement: Model -> Argument -> Html Msg
runFormElement model arg =
    let
        fields =
            [h4 [class "ui dividing header"]
                 [a [class "ui link"
                    ,Page.href (DetailPage model.scriptName)
                    ,onClick (RemoveArgument arg)]
                      [i [class "small trash icon"][]
                      ]
                 ,text "Parameter: "
                 ,code [] [Data.Argument.getName arg |> text]
                 ]
            ,div [class "fields"]
                [div [class "six wide field"]
                     [label [][text "Type"]
                     ,select [onInput (stringToFormat >> SetArgumentType arg)]
                         (List.map
                              (\f -> option
                                   [selected (Data.Argument.isInput arg f)
                                   ,value (formatToString f)
                                   ][text (formatToString f)])
                              Data.Param.allFormats)
                     ]
                ,div [class "ten wide field"]
                    [label [][text "Argument Name"]
                    ,input [type_ "text", value ""
                           , placeholder "optional name"
                           , onInput (SetArgumentName arg)
                           , value (Data.Argument.getName arg)
                           ][]
                    ]
                ]
            ]
    in
        case arg of
            Data.Argument.Upl a ->
                div [class "ui basic segment"]
                    (fields ++
                         [div [class "sixteen wide field"]
                              [label [][text "Content"]
                              ,div [class "ui buttons"]
                                  [button [class "ui secondary icon button"
                                          ,onClick (ClearFiles arg)
                                          ]
                                       [i [class "ui trash icon"][]
                                       ]
                                  ,button [class "ui primary button"
                                          ,onClick (RequestFile arg (a.input == Files))
                                          ]
                                       [text "Select file ..."
                                       ]
                                  ]
                              , div [class "ui container"]
                                  [div [class "ui horizontal list"]
                                       (List.map makeFileItem a.content)
                                  ]
                              ]
                         ]
                    )

            Data.Argument.Str a ->
                case a.input of
                    Text ->
                        div [class "ui basic segment"]
                            (fields ++
                                 [div [class "sixteen wide field"]
                                      [label [][text "Content"]
                                      ,textarea [rows 15
                                                ,onInput (SetArgumentText arg)
                                                ][Data.Argument.getText arg |> text]
                                      ]
                                 ]
                            )
                    _ ->
                        div [class "ui basic segment"]
                            (fields ++
                                 [div [class "sixteen wide field"]
                                      [label [][text "Content"]
                                      ,input [type_ (if Data.Argument.isInput arg Line then "text" else "password")
                                             ,onInput (SetArgumentText arg)
                                             ,value (Data.Argument.getText arg)
                                             ][]
                                      ]
                                 ]
                            )

makeFileItem: File -> Html Msg
makeFileItem file =
    let
        name = File.name file
        size = File.size file
    in
        div [class "item"]
            [div [class "content"]
                [div [class "header"]
                     [text name
                     ]
                ,Util.Size.bytesReadable Util.Size.B (toFloat size) |> text
                ]
            ]


stdout: Model -> Html Msg
stdout model =
    pre [class "script-output"]
       [code []
            [ text model.scriptStdout
            ]
       ]

stderr: Model -> Html Msg
stderr model =
    pre [class "script-output"]
       [code []
            [ text model.scriptStderr
            ]
       ]

content: Model -> Html Msg
content model =
    if model.contentEdit then contentEdit model
    else contentView model

contentEdit: Model -> Html Msg
contentEdit model =
    div []
        [div [class ""]
             [button [class "ui primary right floated button"
                     ,onClick SaveContentEdit
                     ,Page.href (DetailPage model.scriptName)
                     ]
                  [text "Save"
                  ]
             ,button [class "ui button"
                     ,onClick CancelContentEdit
                     ,Page.href (DetailPage model.scriptName)
                     ]
                  [text "Cancel"
                  ]
             ]
        , div [class "ui form"]
            [ textarea [rows 20
                       ,class "script-edit"
                       ,onInput SetScriptContent
                       ]
                  [text model.scriptContent
                  ]
            ]
        ]

contentView: Model -> Html Msg
contentView model =
    div []
        [div [class ""]
             [button [class "ui primary button"
                     ,onClick EnterContentEdit
                     ,Page.href (DetailPage model.scriptName)
                     ]
                  [text "Edit"
                  ]
             ,button [class "ui right floated red button"]
                 [text "Delete"
                 ]
             ]
        , pre []
            [code []
                 [ text model.scriptContent
                 ]
            ]
        ]

labels: Model -> Html Msg
labels model =
    let
        script = Maybe.map .script model.info
        out = Maybe.andThen .output model.info
        lastmod = Maybe.map Data.Script.lastMod script

        prop: a -> (Script -> a) -> a
        prop default getter =
            Maybe.map getter script |> Maybe.withDefault default

        propo: a -> (Output -> a) -> a
        propo default getter =
            Maybe.map getter out |> Maybe.withDefault default

        outputLabel = propo "invisible" (\o -> if o.success then "ui green label" else "ui red label")
    in
    div [class "container"]
        ([ div [classList [("ui blue label", True)
                         ,("invisible", prop True (.schedule >> (\s -> s == "")))
                         ]
              , title "This script is enabled for scheduled execution."
              ]
              [ i [class "calendar outline icon"][]
              , prop "" .schedule |> text
              ]
        , div [ classList [("ui basic blue label", True)
                          ,("invisible", prop True (.notifyMail >> (\c -> c == "")))
                          ]
              , title "Notify this e-mail with every outcome."
              ]
              [ i [class "envelope outline icon"][]
              , prop "" .notifyMail |> text
              ]
        , div [ classList [("ui basic orange label", True)
                          ,("invisible", prop True (.notifyErrorMail >> (\c -> c == "")))
                          ]
              , title "Notify this e-mail on error only."
              ]
              [ i [class "envelope outline icon"][]
              ,  prop "" .notifyErrorMail |> text
              ]
        , div [ class outputLabel
              , title "Last run with running time"
              ]
              [ i [class (if propo False .success then "cog icon" else "bolt icon")][]
              , propo "" .date |> text
              , div [class "detail"][propo 0 .runningTime |> Util.Duration.toHuman |> text]
              ]
        , div [ classList [("ui circular olive label", True)
                          ,("invisible", propo True (\_ -> False))
                          ]
              , title "Successful runs"
              ]
              [ propo 0 .runSuccess |> String.fromInt |> text
              ]
        , div [ classList [("ui circular orange label", True)
                          ,("invisible", propo True (\_ -> False))]
              , title "Erroneous runs"
              ]
              [ propo 0 .runSuccess |> (-) (propo 0 .runCount) |> String.fromInt |> text
              ]
        , div [ class "ui label"
               , title "Last modified"
               ]
              [ i [class "pencil icon"][]
              , Maybe.withDefault "" lastmod |> text
              ]
        ] ++ (categoryTags (prop [] .category)))


categoryTags: List String -> List (Html Msg)
categoryTags tags =
    List.map
        (\c -> div [class "ui tag label", title "Category"]
             [text c]
        )
        tags
