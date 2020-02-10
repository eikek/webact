module Data.ScriptInfo exposing (..)

import Data.Output exposing (Output)
import Data.Script exposing (Script)
import Json.Decode as Decode exposing (Decoder)
import Json.Decode.Pipeline exposing (required)


type alias ScriptInfo =
    { script : Script
    , output : Maybe Output
    }


scriptInfoDecoder : Decoder ScriptInfo
scriptInfoDecoder =
    Decode.succeed ScriptInfo
        |> required "script" Data.Script.scriptDecoder
        |> required "output" (Decode.maybe Data.Output.outputDecoder)


scriptInfoListDecoder : Decoder (List ScriptInfo)
scriptInfoListDecoder =
    Decode.list scriptInfoDecoder
