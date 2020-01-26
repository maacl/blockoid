(ns org.parkerici.blockoid-example.core
  (:require [org.parkerici.blockoid.core :as bo]
            [clojure.pprint :as pprint])
  )

(defn fn-block [f]
  {:type (str f "-fn")
   :message0 (str f "%1 %2")
   :args0 [{:type "field_input"
            :name "a"}
           {:type "field_input"
            :name "b"}]
   :fn f                                ;you can include extra fields
   })

(def blockdefs
  (map fn-block '(+ - * /)))

(def toolbox
  `[:toolbox
    [:category "Arithmetic" {}
     ~@(cons [:block "math_number" [:field "NUM" 123]]
             (mapv (fn [block] [:block (:type block)]) blockdefs))]])

;;; A near-copy of the toolbox in Blockly demo: https://blockly-demo.appspot.com/static/demos/toolbox/index.html
;;; TODO colors

(def demo-toolbox
  [:toolbox
   [:category "Logic" {:color "red"}
    [:category "if" {}
     [:block "controls_if"]
     #_                                 ;TODO
     [:block "controls_if" {}
      [:mutation "else" 1]]           
     ]
    [:category "Boolean" {:color "red"}
     [:block "logic_compare"]
     [:block "logic_operation"]
     [:block "logic_negate"]
     [:block "logic_boolean"]
     [:block "logic_null"]
     [:block "logic_ternary"]]]
   [:category "Loops" {:color "blue"}
    [:block "controls_repeat_ext" {}
     [:value  "TIMES"
      [:block "math_number" {}
       [:field "NUM" "10"]]]]
    [:block "controls_whileUntil" {} ]
    [:block "controls_for"  {}
     [:field "VAR" "i"]
     [:value "FROM"
      [:block  "math_number"  {}
       [:field "NUM" 1]]]
     [:value "TO"
      [:block  "math_number"  {}
       [:field "NUM" 10]]]
     [:value "BY"
      [:block  "math_number"  {}
       [:field "NUM" 1]]]]
    [:block "controls_forEach" {} ]
    [:block "controls_flow_statements" {} ]
    ]
   [:category "Math" {:color "BLACK"}
    [:block "math_number"  {}
     [:field "NUM" 123]]
    [:block "math_arithmetic" {}]
    [:block "math_single" {}]
    [:block "math_trig" {}]
    [:block "math_constant" {}]
    [:block "math_number_property" {}]
    [:block "math_round" {}]
    [:block "math_on_list" {}]
    [:block "math_modulo" {}]
    [:block "math_constrain" {}
     [:value "LOW"
      [:block "math_number" {} 
       [:field "NUM" 1]]]
     [:value "HIGH"
      [:block "math_number" {} 
       [:field "NUM" 100]]]]
    [:block "math_random_int"  {}
     [:value "FROM"
      [:block "math_number" {} 
       [:field "NUM" 1]]]
     [:value "TO"
      [:block "math_number" {} 
       [:field "NUM" 100]]]]
    [:block "math_random_float" {}]
    [:block "math_atan2" {}]
    ]
   [:category "Lists" {}
    [:block "lists_create_empty" {}]
    [:block "lists_create_with" {}]
    [:block
     "lists_repeat"
     {}
     [:field "NUM"
      [:block
       "math_number" {}
       [:field "NUM" 5]]]]
    [:block "lists_length" {}]
    [:block "lists_isEmpty" {}]
    [:block "lists_indexOf" {}]
    ]
   [:sep]
   [:category "Variables" {:custom "VARIABLE"} ;TODO no idea what :custom does
    ]
   ;; Omitting huge Randomize structure out of laziness
   [:category "Jabberwocky" {}
    [:block "text_print" {}
     [:value "TEXT"
      [:block "text" {}
       [:field "TEXT" "'Twas brillig, and the slithy toves"]]]
     [:next
      [:block "text_print" {}
       [:value "TEXT"
        [:block "text" {}
         [:field "TEXT" "  Did gyre and gimble in the wabe:"]]]]]]
    ;; etc
    ]])

(defn initialize []
  (bo/define-blocks blockdefs)
  (bo/define-workspace
    "blocklyDiv"
;    toolbox
    demo-toolbox
    {}
    (fn [_]
      (let [s (with-out-str
                (-> (bo/relevant-xml)
                  bo/compact
                  pprint/pprint))]
        (set! (.-innerHTML (.getElementById js/document "compact")) s)))))


