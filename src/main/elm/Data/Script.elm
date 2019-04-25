module Data.Script exposing (..)

import Time exposing (Posix, Zone, toYear, toMonth, toDay, toHour, toMinute, toSecond)
import Json.Decode as Decode exposing (Decoder, int, string, float, bool)
import Json.Decode.Pipeline exposing (required, optional, hardcoded)
import Data.Param exposing (Param, Format(..))

type alias Script =
    { name: String
    , category: List String
    , lastModified: Int
    , description: String
    , schedule: String
    , scheduledAt: String
    , executing: Int
    , enabled: Bool
    , notifyMail: String
    , notifyErrorMail: String
    , params: List Param
    }


scriptDecoder: Decoder Script
scriptDecoder =
    Decode.succeed Script
        |> required "name" string
        |> required "category" (Decode.list string)
        |> required "lastModified" int
        |> required "description" string
        |> required "schedule" string
        |> required "scheduledAt" string
        |> required "executing" int
        |> required "enabled" bool
        |> required "notifyMail" string
        |> required "notifyErrorMail" string
        |> required "params" (Decode.list Data.Param.paramDecoder)

scriptListDecoder: Decoder (List Script)
scriptListDecoder =
    Decode.list scriptDecoder

lastMod: Script -> String
lastMod script =
    let
        posix = Time.millisToPosix script.lastModified

        write: (Zone -> Posix -> Int) -> String
        write f =
            (f Time.utc posix) |> String.fromInt

        writeMonth: String
        writeMonth =
            case (toMonth Time.utc posix) of
                Time.Jan -> "01"
                Time.Feb -> "02"
                Time.Mar -> "03"
                Time.Apr -> "04"
                Time.May -> "05"
                Time.Jun -> "06"
                Time.Jul -> "07"
                Time.Aug -> "08"
                Time.Sep -> "09"
                Time.Oct -> "10"
                Time.Nov -> "11"
                Time.Dec -> "12"
    in
        (write toYear) ++ "-" ++
        (writeMonth) ++ "-" ++
        (write toDay) ++ " " ++
        (write toHour) ++ ":" ++
        (write toMinute) ++ ":" ++
        (write toSecond) ++ " (Z)"
