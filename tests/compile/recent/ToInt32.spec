        1. Let _number_ be ? ToNumber(_argument_).
        1. If _number_ is *NaN*, *+0*<sub>𝔽</sub>, *-0*<sub>𝔽</sub>, *+∞*<sub>𝔽</sub>, or *-∞*<sub>𝔽</sub>, return *+0*<sub>𝔽</sub>.
        1. Let _int_ be the mathematical value that is the same sign as _number_ and whose magnitude is floor(abs(ℝ(_number_))).
        1. Let _int32bit_ be _int_ modulo 2<sup>32</sup>.
        1. If _int32bit_ ≥ 2<sup>31</sup>, return 𝔽(_int32bit_ - 2<sup>32</sup>); otherwise return 𝔽(_int32bit_).