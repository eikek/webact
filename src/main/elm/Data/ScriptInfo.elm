module Data.ScriptInfo exposing (..)

import Json.Decode as Decode exposing (Decoder, int, string, float, bool)
import Json.Decode.Pipeline exposing (required, optional, hardcoded)

import Data.Script exposing (Script)
import Data.Output exposing (Output)

type alias ScriptInfo =
    { script: Script
    , output: Maybe Output
    }

scriptInfoDecoder: Decoder ScriptInfo
scriptInfoDecoder =
    Decode.succeed ScriptInfo
        |> required "script" Data.Script.scriptDecoder
        |> required "output" (Decode.maybe Data.Output.outputDecoder)

scriptInfoListDecoder: Decoder (List ScriptInfo)
scriptInfoListDecoder =
    Decode.list scriptInfoDecoder
