module Data.Version exposing (..)

import Json.Decode as Decode exposing (Decoder, int, string, float, bool)
import Json.Decode.Pipeline exposing (required, optional, hardcoded)

type alias Version =
    { version: String
    , builtAtMillis: Int
    , builtAtString: String
    , gitCommit: String
    , gitVersion: String
    }

empty: Version
empty =
    { version = "<unknown>"
    , builtAtMillis = 0
    , builtAtString = ""
    , gitCommit = ""
    , gitVersion = ""
    }


versionDecoder: Decoder Version
versionDecoder =
    Decode.succeed Version
        |> required "version" string
        |> required "builtAtMillis" int
        |> required "builtAtString" string
        |> required "gitCommit" string
        |> required "gitVersion" string
