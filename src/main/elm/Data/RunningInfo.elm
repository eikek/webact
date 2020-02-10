module Data.RunningInfo exposing (..)

import Json.Decode as Decode exposing (Decoder, int)
import Json.Decode.Pipeline exposing (required)


type alias RunningInfo =
    { executing : Int
    }


runningInfoDecoder : Decoder RunningInfo
runningInfoDecoder =
    Decode.succeed RunningInfo
        |> required "executing" int
