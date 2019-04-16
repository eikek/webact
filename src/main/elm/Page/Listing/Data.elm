module Page.Listing.Data exposing (..)

import Http
import Data.Script exposing (Script)

type alias Model =
    { list: List Script
    }

emptyModel: Model
emptyModel =
    { list = []
    }

type Msg
    = AllScripts (Result Http.Error (List Script))
