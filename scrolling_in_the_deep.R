library(devtools)
library("rstudioapi")
library(lme4)
library(installr)
library(moments)
library(mgcv)
library(ggplot2)
library(car)
library(MASS)
library(simr)
library(emmeans)
library(broom.mixed)
library(report)
library(stargazer)
library(tidyverse)
library(lmerTest)
library(readr)
library(dplyr)


setwd(dirname(getActiveDocumentContext()$path))

main_df <- read_csv("data_sweetContext.csv")
main_df <- as.data.frame(main_df)
main_df$Responsiveness


# Independent Variables
main_df$Multitasking <- as.factor(main_df$Multitasking)
main_df$Social_Situation <- as.factor(main_df$Social_Situation)
main_df$At_Home <- as.factor(main_df$At_Home)
main_df$App_Name <- as.factor(main_df$App_Name)
main_df$Sleepiness <- as.numeric(main_df$Sleepiness)
main_df$Valence <- as.numeric(main_df$Valence)
main_df$Current_Activity <- as.numeric(main_df$Current_Activity)

# Dependent Variables
main_df$Responsiveness <- as.numeric(main_df$Responsiveness)
main_df$Responsiveness_logTrans <- log1p(main_df$Responsiveness) # do log+1 transformation
main_df$Reactance <- as.numeric(main_df$Reactance)



# Model Reatance
model_reactance_main_effects <- lmer(Reactance ~ At_Home + Current_Activity + Sleepiness + Valence + Multitasking + Social_Situation + (1|Participant_ID), data = main_df)
model_reactance_interaction_effects <- lmer(Reactance ~ At_Home * Current_Activity * Sleepiness * Valence * Multitasking * Social_Situation + (1|Participant_ID), data = main_df)

summary(model_reactance_main_effects)
summary(model_reactance_interaction_effects)


# Model Responsiveness
model_responsiveness_main_effects <- lmer(Responsiveness_logTrans ~ At_Home + Current_Activity + Sleepiness + Valence + Multitasking + Social_Situation + (1|Participant_ID), data = main_df)
model_responsiveness_interaction_effects <- lmer(Responsiveness_logTrans ~ At_Home * Current_Activity * Sleepiness * Valence * Multitasking * Social_Situation + (1|Participant_ID), data = main_df)

summary(model_responsiveness_main_effects)
summary(model_responsiveness_interaction_effects)



# 
# Model Comparison
# 
anova(model_reactance_main_effects, model_reactance_interaction_effects)
anova(model_responsiveness_main_effects, model_responsiveness_interaction_effects)




# 
# Descriptive Data
# 

mean(main_df$Reactance, na.rm = TRUE)
sd(main_df$Reactance, na.rm = TRUE)
min(main_df$Reactance, na.rm = TRUE)
max(main_df$Reactance, na.rm = TRUE)


mean(main_df$Responsiveness, na.rm = TRUE)
sd(main_df$Responsiveness, na.rm = TRUE)
min(main_df$Responsiveness, na.rm = TRUE)
max(main_df$Responsiveness, na.rm = TRUE)


# 
# Distributions
# 

occurrences <- table(main_df$App_Name)
percentages <- (occurrences / sum(occurrences)) * 100
print(percentages)




