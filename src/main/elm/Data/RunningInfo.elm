module Data.RunningInfo exposing (..)

import Json.Decode as Decode exposing (Decoder, int, string, float, bool)
import Json.Decode.Pipeline exposing (required, optional, hardcoded)

type alias RunningInfo =
    { executing: Int
    }

runningInfoDecoder: Decoder RunningInfo
runningInfoDecoder =
    Decode.succeed RunningInfo
        |> required "executing" int
