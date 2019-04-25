module Page.Create.Update exposing (update)

import Api
import Page.Create.Data exposing (..)

update: Msg -> Model -> (Model, Cmd Msg)
update msg model =
    case msg of
        SetName n ->
            ({ model | name = n, nameValid = isNameValid n }
            , if n == "" then Cmd.none
              else Api.scriptDetail model.flags.apiBase n ScriptGetRes
            )
        SetContent s ->
            ({ model | content = s, contentValid = isContentValid s }
            ,Cmd.none
            )
        ScriptGetRes (Ok _) ->
            ({ model | nameExists = True}
            ,Cmd.none
            )
        ScriptGetRes (Err err) ->
            ({ model | nameExists = False }
            ,Cmd.none
            )
        SaveScript ->
            if model.contentValid && model.nameValid then
                (model
                ,Api.scriptUpload
                    model.flags.apiBase
                    model.name
                    model.content
                    SaveScriptRes
                )
            else
                (model, Cmd.none)

        SaveScriptRes (Ok ()) ->
            ({model | saved = Just True}
            , Cmd.none
            )
        SaveScriptRes (Err err) ->
            ({model | saved = Just False}
            , Cmd.none
            )

isNameValid: String -> Bool
isNameValid name =
    if name == "" then False
    else True

isContentValid: String -> Bool
isContentValid cnt =
    if cnt == "" then False
    else True
