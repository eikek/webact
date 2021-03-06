module Data.Output exposing (..)

import Json.Decode as Decode exposing (Decoder, bool, float, int, string)
import Json.Decode.Pipeline exposing (hardcoded, optional, required)


type alias Output =
    { date : String
    , returnCode : Int
    , success : Bool
    , runningTime : Int
    , runCount : Int
    , runSuccess : Int
    }


outputDecoder : Decoder Output
outputDecoder =
    Decode.succeed Output
        |> required "date" string
        |> required "returnCode" int
        |> required "success" bool
        |> required "runningTime" int
        |> required "runCount" int
        |> required "runSuccess" int


outputListDecoder : Decoder (List Output)
outputListDecoder =
    Decode.list outputDecoder
