package com.attask.jenkins.testreport;

import hudson.*;
import hudson.model.*;
import org.kohsuke.stapler.*;

import java.io.*;
import java.util.regex.*;

/**
 * Created with IntelliJ IDEA.
 * User: josephbass
 * Date: 11/22/13
 * Time: 11:03 AM
 */
public class HighlightStyle extends AbstractDescribableImpl<HighlightStyle> implements Serializable {
	private String regex;
	private String color;
	private Pattern pattern;

	@DataBoundConstructor
	public HighlightStyle(String regex, String color) {
		this.regex = regex;
		this.color = color;
		try {
			this.pattern = Pattern.compile(regex);
		} catch (PatternSyntaxException e) {
			e.printStackTrace();
			this.pattern = null;
		}
	}

	public String getRegex() {
		return regex;
	}

	public String getColor() {
		return color;
	}

	@Override
	public String toString() {
		return regex + ", " + color;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof HighlightStyle) {
			HighlightStyle other = (HighlightStyle) o;
			if (other.getRegex() == null) {
				if (this.getRegex() != null) {
					return false;
				}
				if (other.getColor() == null) {
					if (this.getColor() != null) {
						return false;
					}

				} else {
					return this.getColor().equals(other.getColor());
				}
			} else {
				return this.getRegex().equals(other.getRegex()) && this.getColor().equals(other.getColor());
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		if (this.getRegex() != null) {
			if (this.getColor() != null) {
				return this.getRegex().hashCode() + this.getColor().hashCode();
			} else {
				return this.getRegex().hashCode();
			}
		} else if (this.getColor() != null) {
			if (this.getRegex() != null) {
				return this.getRegex().hashCode() + this.getColor().hashCode();
			} else {
				return this.getColor().hashCode();
			}
		} else {
			return 0;
		}
	}

	public Pattern getPattern() {
		return pattern;
	}

	@Extension
        public static final class DescriptorImpl extends Descriptor<HighlightStyle> {
                @Override
                public String getDisplayName() {
                        return "Highlight Style";
                }
        }
}
