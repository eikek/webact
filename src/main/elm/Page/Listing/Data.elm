module Page.Listing.Data exposing (..)

import Http
import Data.ScriptInfo exposing (ScriptInfo)

type alias Model =
    { scripts: List ScriptInfo
    , category: String
    }

emptyModel: Model
emptyModel =
    { scripts = []
    , category = "All"
    }

type Msg
    = AllScripts (Result Http.Error (List ScriptInfo))
    | SetCategory String
