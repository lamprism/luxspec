/*
 * Copyright (C) Lamprism
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lamprism.luxspec.console.help.text;

import java.util.Objects;

/**
 * Immutable formatting policy for the standard plain-text Help renderer.
 *
 * @author RollW
 */
public final class PlainTextHelpFormat {
    private final int width;
    private final int itemIndent;
    private final int descriptionGap;
    private final boolean showAliases;
    private final boolean showRequired;
    private final boolean showDefaults;
    private final boolean showHidden;
    private final String usageHeading;
    private final String commandsHeading;
    private final String argumentsHeading;
    private final String optionsHeading;
    private final String examplesHeading;
    private final String notesHeading;

    private PlainTextHelpFormat(Builder builder) {
        this.width = builder.width;
        this.itemIndent = builder.itemIndent;
        this.descriptionGap = builder.descriptionGap;
        this.showAliases = builder.showAliases;
        this.showRequired = builder.showRequired;
        this.showDefaults = builder.showDefaults;
        this.showHidden = builder.showHidden;
        this.usageHeading = builder.usageHeading;
        this.commandsHeading = builder.commandsHeading;
        this.argumentsHeading = builder.argumentsHeading;
        this.optionsHeading = builder.optionsHeading;
        this.examplesHeading = builder.examplesHeading;
        this.notesHeading = builder.notesHeading;
    }

    /**
     * @return default plain-text formatting
     */
    public static PlainTextHelpFormat defaults() {
        return builder().build();
    }

    /**
     * @return a formatting builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return maximum preferred line width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return number of spaces before section items
     */
    public int getItemIndent() {
        return itemIndent;
    }

    /**
     * @return spaces between an item label and its description
     */
    public int getDescriptionGap() {
        return descriptionGap;
    }

    /**
     * @return whether command aliases are rendered
     */
    public boolean isShowAliases() {
        return showAliases;
    }

    /**
     * @return whether required markers are rendered
     */
    public boolean isShowRequired() {
        return showRequired;
    }

    /**
     * @return whether default values are rendered
     */
    public boolean isShowDefaults() {
        return showDefaults;
    }

    /**
     * @return whether hidden entries are rendered
     */
    public boolean isShowHidden() {
        return showHidden;
    }

    /**
     * @return the usage section heading
     */
    public String getUsageHeading() {
        return usageHeading;
    }

    /**
     * @return the command section heading
     */
    public String getCommandsHeading() {
        return commandsHeading;
    }

    /**
     * @return the positional argument section heading
     */
    public String getArgumentsHeading() {
        return argumentsHeading;
    }

    /**
     * @return the option section heading
     */
    public String getOptionsHeading() {
        return optionsHeading;
    }

    /**
     * @return the example section heading
     */
    public String getExamplesHeading() {
        return examplesHeading;
    }

    /**
     * @return the notes section heading
     */
    public String getNotesHeading() {
        return notesHeading;
    }

    /**
     * @return a builder initialized from this format
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Builder for the standard Help formatting policy.
     */
    public static final class Builder {
        private int width = 100;
        private int itemIndent = 2;
        private int descriptionGap = 3;
        private boolean showAliases = true;
        private boolean showRequired = true;
        private boolean showDefaults = true;
        private boolean showHidden;
        private String usageHeading = "Usage";
        private String commandsHeading = "Commands";
        private String argumentsHeading = "Arguments";
        private String optionsHeading = "Options";
        private String examplesHeading = "Examples";
        private String notesHeading = "Notes";

        private Builder() {
        }

        private Builder(PlainTextHelpFormat format) {
            this.width = format.width;
            this.itemIndent = format.itemIndent;
            this.descriptionGap = format.descriptionGap;
            this.showAliases = format.showAliases;
            this.showRequired = format.showRequired;
            this.showDefaults = format.showDefaults;
            this.showHidden = format.showHidden;
            this.usageHeading = format.usageHeading;
            this.commandsHeading = format.commandsHeading;
            this.argumentsHeading = format.argumentsHeading;
            this.optionsHeading = format.optionsHeading;
            this.examplesHeading = format.examplesHeading;
            this.notesHeading = format.notesHeading;
        }

        /**
         * Sets the preferred maximum line width.
         *
         * @param width preferred maximum line width
         * @return this builder
         */
        public Builder width(int width) {
            if (width < 20) {
                throw new IllegalArgumentException("Help width must be at least 20");
            }
            this.width = width;
            return this;
        }

        /**
         * Sets the indentation for section items.
         *
         * @param itemIndent number of spaces before section items
         * @return this builder
         */
        public Builder itemIndent(int itemIndent) {
            if (itemIndent < 0) {
                throw new IllegalArgumentException("Help item indent cannot be negative");
            }
            this.itemIndent = itemIndent;
            return this;
        }

        /**
         * Sets the gap between an item label and its description.
         *
         * @param descriptionGap number of spaces between columns
         * @return this builder
         */
        public Builder descriptionGap(int descriptionGap) {
            if (descriptionGap < 1) {
                throw new IllegalArgumentException("Help description gap must be positive");
            }
            this.descriptionGap = descriptionGap;
            return this;
        }

        /**
         * Sets whether aliases are rendered.
         *
         * @param showAliases whether aliases should be rendered
         * @return this builder
         */
        public Builder showAliases(boolean showAliases) {
            this.showAliases = showAliases;
            return this;
        }

        /**
         * Sets whether required metadata is rendered.
         *
         * @param showRequired whether required metadata should be rendered
         * @return this builder
         */
        public Builder showRequired(boolean showRequired) {
            this.showRequired = showRequired;
            return this;
        }

        /**
         * Sets whether default values are rendered.
         *
         * @param showDefaults whether default values should be rendered
         * @return this builder
         */
        public Builder showDefaults(boolean showDefaults) {
            this.showDefaults = showDefaults;
            return this;
        }

        /**
         * Sets whether hidden entries are rendered.
         *
         * @param showHidden whether hidden entries should be rendered
         * @return this builder
         */
        public Builder showHidden(boolean showHidden) {
            this.showHidden = showHidden;
            return this;
        }

        /**
         * Sets the usage section heading.
         *
         * @param usageHeading usage section heading
         * @return this builder
         */
        public Builder usageHeading(String usageHeading) {
            this.usageHeading = requireHeading(usageHeading, "Usage heading");
            return this;
        }

        /**
         * Sets the command section heading.
         *
         * @param commandsHeading command section heading
         * @return this builder
         */
        public Builder commandsHeading(String commandsHeading) {
            this.commandsHeading = requireHeading(commandsHeading, "Commands heading");
            return this;
        }

        /**
         * Sets the positional argument section heading.
         *
         * @param argumentsHeading positional argument section heading
         * @return this builder
         */
        public Builder argumentsHeading(String argumentsHeading) {
            this.argumentsHeading = requireHeading(argumentsHeading, "Arguments heading");
            return this;
        }

        /**
         * Sets the option section heading.
         *
         * @param optionsHeading option section heading
         * @return this builder
         */
        public Builder optionsHeading(String optionsHeading) {
            this.optionsHeading = requireHeading(optionsHeading, "Options heading");
            return this;
        }

        /**
         * Sets the example section heading.
         *
         * @param examplesHeading example section heading
         * @return this builder
         */
        public Builder examplesHeading(String examplesHeading) {
            this.examplesHeading = requireHeading(examplesHeading, "Examples heading");
            return this;
        }

        /**
         * Sets the notes section heading.
         *
         * @param notesHeading notes section heading
         * @return this builder
         */
        public Builder notesHeading(String notesHeading) {
            this.notesHeading = requireHeading(notesHeading, "Notes heading");
            return this;
        }

        /**
         * Builds the immutable formatting policy.
         *
         * @return the immutable Help format
         */
        public PlainTextHelpFormat build() {
            return new PlainTextHelpFormat(this);
        }

        private static String requireHeading(String value, String name) {
            Objects.requireNonNull(value, name);
            String normalized = value.strip();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException(name + " cannot be blank");
            }
            return normalized;
        }
    }
}
