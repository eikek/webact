module Page.Detail.Update exposing (update)

import File.Select as Select
import Api
import Ports
import Page.Detail.Data exposing (..)
import Data.Argument exposing (Argument)

update: Msg -> Model -> (Model, Cmd Msg)
update msg model =
    case msg of
        ChangeName n ->
            ( {model | scriptName = n }
            , initCmd model n
            )

        ChangeTab Content ->
            ( {model | tab = Content }
            , if model.scriptContent /= "" then Cmd.none
              else Api.scriptContent model.baseurl model.scriptName ScriptCnt
            )
        ChangeTab Stdout ->
            ( {model | tab = Stdout }
            , if model.scriptStdout /= "" then Cmd.none
              else Api.scriptStdout model.baseurl model.scriptName ScriptOut
            )
        ChangeTab Stderr ->
            ( {model | tab = Stderr }
            , if model.scriptStderr /= "" then Cmd.none
              else Api.scriptStderr model.baseurl model.scriptName ScriptErr
            )
        ChangeTab tab ->
            ( {model | tab = tab }
            , Cmd.none
            )

        ScriptDetailRes (Ok info) ->
            ( {model | info = Just info
              , contentEdit = False
              , tab = if model.contentEdit then Content else Run
              , runArgs = List.map Data.Argument.fromParam info.script.params
              , scriptContent = if model.contentEdit then model.scriptContent else ""
              , scriptStderr = ""
              , scriptStdout = "" }
            , if info.script.executing == 0 then Cmd.none
              else Api.runningState model.baseurl model.scriptName RunningStateRes
            )
        ScriptDetailRes (Err err) ->
            (model, Cmd.none)

        RunningStateRes (Ok ri) ->
            let
                ns = Maybe.map .script model.info
                     |> Maybe.map (\s -> {s|executing = ri.executing})
                ni = Maybe.map (\i -> {i | script = Maybe.withDefault i.script ns}) model.info
            in
            ( {model | info = ni }
            , if ri.executing == 0 then Api.scriptDetail model.baseurl model.scriptName ScriptDetailRes
              else Api.runningState model.baseurl model.scriptName RunningStateRes
            )

        RunningStateRes (Err err) ->
            (model, Cmd.none)

        ScriptCnt (Ok cnt) ->
            ({model | scriptContent = cnt}, Cmd.none)

        ScriptCnt (Err err) ->
            (model, Cmd.none)

        ScriptOut (Ok cnt) ->
            ({model | scriptStdout = cnt}, Cmd.none)

        ScriptOut (Err err) ->
            (model, Cmd.none)

        ScriptErr (Ok cnt) ->
            ({model | scriptStderr = cnt}, Cmd.none)

        ScriptErr (Err err) ->
            (model, Cmd.none)

        EnterContentEdit ->
            ( { model | contentEdit = True }
            , Cmd.none
            )
        CancelContentEdit ->
            ( { model | contentEdit = False }
            , Cmd.none
            )
        SaveContentEdit ->
            (model
            , Api.scriptUpload
                model.baseurl
                model.scriptName
                model.scriptContent ContentSaved)
        SetScriptContent cnt ->
            ({model | scriptContent = cnt, scriptStdout = "", scriptStderr = "" }
            ,Cmd.none
            )

        ContentSaved (Ok ()) ->
            ( model
            , Api.scriptDetail model.baseurl model.scriptName ScriptDetailRes
            )
        ContentSaved (Err err) ->
            (model, Cmd.none)

        AddArgument ->
            ( appendArgument model
            , Cmd.none
            )
        RemoveArgument arg ->
            ( {model | runArgs = Data.Argument.removeArg arg model.runArgs}
            , Cmd.none
            )
        SetArgumentName arg str ->
            let
                narg = Data.Argument.setName str arg
            in
            (replaceArgument narg model, Cmd.none)
        SetArgumentText arg str ->
            let
                narg = Data.Argument.setText str arg
            in
            (replaceArgument narg model, Cmd.none)
        SetArgumentType arg el ->
            let
                narg = Data.Argument.setInput el arg
            in
                (replaceArgument narg model, Cmd.none)

        RunScript ->
            ( {model| scriptStdout = "", scriptStderr = ""}
            , Api.runScript
                model.baseurl
                model.scriptName
                model.runArgs
                RunScriptRes
            )
        RunScriptRes (Ok ()) ->
            ({model| scriptStdout = "", scriptStderr = ""}
            , Api.scriptDetail model.baseurl model.scriptName ScriptDetailRes
            )
        RunScriptRes (Err err) ->
            ( {model| scriptStdout = "", scriptStderr = ""}
            , Cmd.none
            )

        RequestFile arg multi ->
            ( model
            , if multi then Select.files [] (\f -> \fl -> FileSelected arg fl f)
              else Select.file [] (FileSelected arg [])
            )

        FileSelected arg list file ->
            let
                narg = Data.Argument.setFile (file :: list) arg
            in
                (replaceArgument narg model, Cmd.none)

        ClearFiles arg ->
            let
                narg = Data.Argument.setFile [] arg
            in
                (replaceArgument narg model, Cmd.none)
