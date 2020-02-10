module Data.Param exposing (..)

import Json.Decode as Decode exposing (Decoder, string)
import Json.Decode.Pipeline exposing (required)


type alias Param =
    { name : String
    , format : Format
    }


type Format
    = Line
    | Text
    | Password
    | File
    | Files


allFormats : List Format
allFormats =
    [ Line
    , Text
    , Password
    , File
    , Files
    ]


formatToString : Format -> String
formatToString format =
    case format of
        Line ->
            "Line"

        Text ->
            "Text"

        Password ->
            "Password"

        File ->
            "File"

        Files ->
            "Files"


stringToFormat : String -> Format
stringToFormat str =
    case String.toLower str of
        "line" ->
            Line

        "text" ->
            Text

        "password" ->
            Password

        "file" ->
            File

        "files" ->
            Files

        _ ->
            Line


paramDecoder : Decoder Param
paramDecoder =
    Decode.succeed Param
        |> required "name" string
        |> required "format" formatDecoder


formatDecoder : Decoder Format
formatDecoder =
    Decode.map stringToFormat string
