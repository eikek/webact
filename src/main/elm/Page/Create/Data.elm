module Page.Create.Data exposing (..)

import Http
import Data.Flags exposing (Flags)
import Data.ScriptInfo exposing (ScriptInfo)

type alias Model =
    { name: String
    , content: String
    , nameExists: Bool
    , flags: Flags
    , saved: Maybe Bool
    , nameValid: Bool
    , contentValid: Bool
    }

emptyModel: Flags -> Model
emptyModel flags =
    { name = ""
    , content = ""
    , nameExists = False
    , flags = flags
    , saved = Nothing
    , nameValid = False
    , contentValid = False
    }

type Msg
    = SetName String
    | SetContent String
    | ScriptGetRes (Result Http.Error ScriptInfo)
    | SaveScript
    | SaveScriptRes (Result Http.Error ())
