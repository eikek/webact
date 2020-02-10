module Page.Detail.Data exposing (..)

import Api
import Comp.YesNoDimmer
import Data.Argument exposing (..)
import Data.Param exposing (Format)
import Data.RunningInfo exposing (RunningInfo)
import Data.ScriptInfo exposing (ScriptInfo)
import File exposing (File)
import Http


type alias Model =
    { scriptName : String
    , scriptContent : String
    , scriptStdout : String
    , scriptStderr : String
    , info : Maybe ScriptInfo
    , baseurl : String
    , tab : Tab
    , contentEdit : Bool
    , runArgs : List Argument
    , deleteConfirm : Comp.YesNoDimmer.Model
    }


emptyModel : String -> String -> Model
emptyModel baseurl name =
    { scriptName = name
    , scriptContent = ""
    , scriptStdout = ""
    , scriptStderr = ""
    , info = Nothing
    , baseurl = baseurl
    , tab = Run
    , contentEdit = False
    , runArgs = []
    , deleteConfirm = Comp.YesNoDimmer.emptyModel
    }


type Tab
    = Run
    | Content
    | Stdout
    | Stderr


tabToString : Tab -> String
tabToString tab =
    case tab of
        Run ->
            "Run"

        Content ->
            "Content"

        Stdout ->
            "Stdout"

        Stderr ->
            "Stderr"


appendArgument : Model -> Model
appendArgument model =
    { model | runArgs = Data.Argument.appendArg (Data.Argument.makeTextArg "" "" 0) model.runArgs }


replaceArgument : Argument -> Model -> Model
replaceArgument arg model =
    { model | runArgs = Data.Argument.replaceArg arg model.runArgs }


type Msg
    = ChangeName String
    | ChangeTab Tab
    | ScriptDetailRes (Result Http.Error ScriptInfo)
    | ScriptOut (Result Http.Error String)
    | ScriptErr (Result Http.Error String)
    | ScriptCnt (Result Http.Error String)
    | EnterContentEdit
    | CancelContentEdit
    | SaveContentEdit
    | ContentSaved (Result Http.Error ())
    | SetScriptContent String
    | AddArgument
    | RemoveArgument Argument
    | SetArgumentName Argument String
    | SetArgumentText Argument String
    | SetArgumentType Argument Format
    | RunScript
    | RunScriptRes (Result Http.Error ())
    | RunningStateRes (Result Http.Error RunningInfo)
    | RequestFile Argument Bool
    | FileSelected Argument (List File) File
    | ClearFiles Argument
    | DeleteScript
    | DeleteScriptResp (Result Http.Error ())
    | DeleteConfirmMsg Comp.YesNoDimmer.Msg


initCmd : Model -> String -> Cmd Msg
initCmd model name =
    if model.scriptName == name then
        Cmd.none

    else
        Api.scriptDetail model.baseurl name ScriptDetailRes
