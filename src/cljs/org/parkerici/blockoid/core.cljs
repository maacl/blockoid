(ns org.parkerici.blockoid.core
  (:require
   cljsjs.blockly
   cljsjs.blockly.blocks
   [re-frame.core :as rf]
   [clojure.data.xml :as xml]))

;;; A thin, application-independent Clojurescript API for Blockly.

;;; ⊓⊔⊓⊔ Workspace ⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔

(def workspace (atom nil))              ; Holds the Blockly workspace object.
(def blockly-div (atom nil))            ; Holds the div containing Blockly
(def callback-keys (atom {}))           ; Maps names to handlers

(defn define-workspace
  "Initialize Blockly. Args:
  div:  Name of the HTML div element in which to inject Blockly
 toolbox-xml: XML in EDN format. Usually generated by `toolbox`. 
 options: map of additional options to `inject`, eg: `{:zoom true}`. See https://developers.google.com/blockly/guides/get-started/web#configuration
   
 change-handler: a fn of one argument (event) that gets called on any changes to the workspace. "
  [div toolbox-xml options change-handler]
  (reset! workspace            ;; see options: https://developers.google.com/blockly/guides/get-started/web#configuration
          (.inject js/Blockly div (clj->js (merge {:toolbox (xml/emit-str toolbox-xml)} options))))
  (reset! blockly-div div)
  ;; Add button handlers
  (doseq [[ckey handler] @callback-keys]
    (.registerButtonCallback @workspace ckey handler))
  (.addChangeListener @workspace change-handler))

(defn update-toolbox
  "Update the toolbox, keeping workspace the same (warning: dangerous)"
  [toolbox-def]
  (.updateToolbox @workspace (xml/emit-str toolbox-def)))

;;; ⊓⊔⊓⊔ Workspace resizing ⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔

;;; This insane rigamarole is to resize the blockly area,
;;; see https://developers.google.com/blockly/guides/configure/web/resizable

(def area-div (atom nil))

(defn- on-resize
  "Compute the absolute coordinates and dimensions of blocklyArea"
  [_]
  (let [blockly-area (.getElementById js/document @area-div)
        blockly-div (.getElementById js/document @blockly-div)]
    (loop [element blockly-area
           x 0
           y 0]
      (if element
        (recur (.-offsetParent element)
               (+ x (.-offsetLeft element))
               (+ y (.-offsetTop element)))
        (do
          (set! (.-left (.-style blockly-div)) (str x "px"))
          (set! (.-top (.-style blockly-div)) (str y "px"))
          (set! (.-width (.-style blockly-div)) (str (.-offsetWidth blockly-area) "px"))
          (set! (.-height (.-style blockly-div)) (str (.-offsetHeight blockly-area) "px"))
          (.svgResize js/Blockly @workspace))))))

(defn auto-resize-workspace
  ;; TODO ugly, make it an option to define-workspace
  "Call this after `define-workspace` to enable automatic resizing"
  [div]
  (reset! area-div div)
  (.addEventListener js/window "resize" on-resize false)
  (on-resize nil))

;;; ⊓⊔⊓⊔ Workspace content ⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔

(defn get-block
  [id]
  (.getBlockById @workspace id))

(defn block-xml-string
  [block]
  (->> block
       (.blockToDom js/Blockly.Xml)
       (.domToText js/Blockly.Xml)))

(defn block-xml
  [block]
  (xml/parse-str (block-xml-string block)))

(defn workspace-xml-string
  []
  (->> @workspace
       (.workspaceToDom js/Blockly.Xml)
       (.domToText js/Blockly.Xml)))

(defn workspace-xml
  []
  (xml/parse-str (workspace-xml-string)))

(defn clear-workspace
  []
  (.clear @workspace))

(defn- encode-xml
  [xml]
  (let [xml-string (xml/emit-str xml)]
    (.textToDom js/Blockly.Xml xml-string)))
  
(defn set-workspace-xml
  [xml]
  (let [dom (encode-xml xml)]
    (clear-workspace)
    (.domToWorkspace js/Blockly.Xml dom @workspace)
    ))

(defn add-workspace-xml
  [xml]
  (let [dom (encode-xml xml)]
    (.appendDomToWorkspace js/Blockly.Xml dom @workspace)))

;;; ⊓⊔⊓⊔ Selection ⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔

(defn selected-block
  []
  (.-selected js/Blockly))

(defn- root-block
  [block]
  (if-let [parent (.getParent block)]
    (root-block parent)
    block))

(defn workspace-selected-xml
  "Returns the XML of the selected block group, or the upper left one if none is selected"
  []
  (if-let [selected (selected-block)]
    (block-xml (root-block selected))
    (first (:content (workspace-xml)))))

;;; ⊓⊔⊓⊔ Custom Blocks ⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔

(defn define-blocks
  "Define new block types. Blockdefs is a seq of maps that are converted to JSON as per https://developers.google.com/blockly/guides/configure/web/custom-blocks"
  [blockdefs]
  (.defineBlocksWithJsonArray js/Blockly (clj->js blockdefs)))

;;; ⊓⊔⊓⊔ Toolbox ⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔
;;; The toolbox method converts an EDN language into the XML required by Blockly to define the toolbox
;;; See https://developers.google.com/blockly/guides/configure/web/toolbox

(defmulti toolbox
  "Translate a toolbox definition form into EDNified XML"
  (fn [[type & _]] type))

(defmethod toolbox :toolbox
  [[_ & contents]]
  {:tag "xml"
   :content (mapv toolbox contents)})

(defmethod toolbox :category
  [[_ name props & contents :as elt]]
  (assert (or (nil? props) (map? props)) ;catch a common error
          (str "Can't parse: " elt))
  {:tag :category
   :attrs (merge
           {:name name
            :expanded true}
           props)
   :content (mapv toolbox contents)})

(defn callback-key [label handler]
  (let [ckey (str label "ButtonHandler")] ;any button-unique name
    (swap! callback-keys assoc ckey handler)
   ckey))

(defmethod toolbox :button
  [[_ label event]]
  {:tag :button
   :attrs {:text label
           :callbackKey (callback-key label #(rf/dispatch event))}})

(defmethod toolbox :block
  [[_ type & [props & subs] :as elt]]
  (assert (or (nil? props) (map? props)) ;catch a common error
          (str "Can't parse: " elt))
  {:tag :block
   :attrs (merge {:type type} props)
   :content (mapv toolbox subs)})

(defmethod toolbox :field
  [[_ name value]]
  {:tag :field
   :attrs {:name name}
   :content (str value)})

(defmethod toolbox :value
  [[_ name & subs]]
  {:tag :value
   :attrs {:name name}
   :content (mapv toolbox subs)})

(defmethod toolbox :next
  [[_ block]]
  {:tag :next
   :content [(toolbox block)]})

(defmethod toolbox :sep
  [[_ _ & _]]
  {:tag :sep})

(defmethod toolbox :default
  [elt]
  (prn "No toolbox translation for: " elt))

;;; TODO shadow is exactly the same as block

;;; ⊓⊔⊓⊔ Compaction ⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔⊓⊔

;;; Compact form is an EDN representation of block structure that is significantly smaller and more
;;; usable than the native XML. Documented here [TODO].
;;; TODO for consistency maybe redo with a multimethod

(defn compact
  "Turns raw Block XML into compact form"
  [block-xml]
  (if (map? block-xml)
    (case (name (:tag block-xml))
      "xml"
      (map compact (:content block-xml))
      ("field" "value" "statement")
      {(get-in block-xml [:attrs :name]) 
       (compact (first (:content block-xml)))}
      "next"
      {:next (compact (first (:content block-xml)))} ;TODO ??? new
      "block"
      {:type (get-in block-xml [:attrs :type])
       :id (get-in block-xml [:attrs :id]) ;TODO make optional?
       :children (apply merge (map compact (:content block-xml)))}
      (throw (ex-info "Couldn't interpret Blockly XML"
                      {:xml block-xml})))
    block-xml))

;;; TODO compact → Blockly


