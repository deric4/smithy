$version: "0.4.0"

use smithy.api#required
use smithy.api#sensitive
use smithy.api#deprecated

namespace smithy.example

structure Struct {
    @required
    @sensitive
    @deprecated
    a: String,
}
