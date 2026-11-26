--[[
formulaLab bridge for the KOReader home screen (Lua).

- Long-press any file or folder in the file manager -> "Render name as formula":
  the name is sent to the formulaLab Android app and rendered as LaTeX.
- Main menu -> Tools -> "formulaLab: render LaTeX": type LaTeX and render it.
- Other Lua code in the shell can call (KOReader registers plugins on the UI by name):
    self.ui.formulalab.renderName("E = mc^2.latex")
    self.ui.formulalab.renderLatex("\\partial_t(\\text{Entropy}) \\geq 0")

Transport: an Android VIEW link, formulalab://render?name=... or ?latex=...
]]

local Device = require("device")
local InfoMessage = require("ui/widget/infomessage")
local InputDialog = require("ui/widget/inputdialog")
local UIManager = require("ui/uimanager")
local WidgetContainer = require("ui/widget/container/widgetcontainer")
local _ = require("gettext")

local FormulaLab = WidgetContainer:extend{
    name = "formulalab",
    is_doc_only = false,
}

local function encode(s)
    return (s:gsub("[^%w%-%._~]", function(c)
        return string.format("%%%02X", c:byte())
    end))
end

local function open(query)
    local url = "formulalab://render?" .. query
    if Device.canOpenLink and Device:canOpenLink() then
        Device:openLink(url)
    else
        UIManager:show(InfoMessage:new{ text = _("formulaLab needs Android.") })
    end
end

-- path or bare name; the app strips the folder and the .latex/.tex/.formula suffix
function FormulaLab.renderName(name)
    local base = name:match("([^/]+)/*$") or name
    open("name=" .. encode(base))
end

function FormulaLab.renderLatex(tex)
    open("latex=" .. encode(tex))
end

function FormulaLab:init()
    self.ui.menu:registerToMainMenu(self)
    if self.ui.addFileDialogButtons then
        self.ui:addFileDialogButtons("formulalab", function(file)
            return {
                {
                    text = _("Render name as formula"),
                    callback = function()
                        if self.ui.file_dialog then UIManager:close(self.ui.file_dialog) end
                        local fc = self.ui.file_chooser
                        if fc and fc.file_dialog then UIManager:close(fc.file_dialog) end
                        FormulaLab.renderName(file)
                    end,
                },
            }
        end)
    end
end

function FormulaLab:onCloseWidget()
    if self.ui.removeFileDialogButtons then
        self.ui:removeFileDialogButtons("formulalab")
    end
end

function FormulaLab:addToMainMenu(menu_items)
    menu_items.formulalab = {
        text = _("formulaLab: render LaTeX"),
        sorting_hint = "tools",
        callback = function()
            local dialog
            dialog = InputDialog:new{
                title = _("LaTeX or file name"),
                buttons = {
                    {
                        {
                            text = _("Cancel"),
                            id = "close",
                            callback = function() UIManager:close(dialog) end,
                        },
                        {
                            text = _("Render"),
                            is_enter_default = true,
                            callback = function()
                                local t = dialog:getInputText()
                                UIManager:close(dialog)
                                if t and t ~= "" then FormulaLab.renderLatex(t) end
                            end,
                        },
                    },
                },
            }
            UIManager:show(dialog)
            dialog:onShowKeyboard()
        end,
    }
end

return FormulaLab
