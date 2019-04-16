module Api exposing (..)

import Http
import Data.Script exposing (Script)

fetchScripts: String -> ((Result Http.Error (List Script)) -> msg) -> Cmd msg
fetchScripts baseurl receive =
    Http.get
        { url = (baseurl ++ "/scripts")
        , expect = Http.expectJson receive Data.Script.scriptListDecoder
        }
