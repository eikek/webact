module Data.Script exposing (..)

import Json.Decode as Decode exposing (Decoder, int, string, float, bool)
import Json.Decode.Pipeline exposing (required, optional, hardcoded)

type alias Script =
    { name: String
    , category: String
    , lastModified: Int
    , description: String
    , schedule: String
    , enabled: Bool
    , notifyMail: String
    , notifyErrorMail: String
    }


scriptDecoder: Decoder Script
scriptDecoder =
    Decode.succeed Script
        |> required "name" string
        |> required "category" string
        |> required "lastModified" int
        |> required "description" string
        |> required "schedule" string
        |> required "enabled" bool
        |> required "notifyMail" string
        |> required "notifyErrorMail" string

scriptListDecoder: Decoder (List Script)
scriptListDecoder =
    Decode.list scriptDecoder
