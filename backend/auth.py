from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from database import get_db
import models, schemas, auth_utils

router = APIRouter(prefix="/auth", tags=["Authentication"])

@router.post("/register", response_model=schemas.UserResponse)
def register(user: schemas.UserCreate, db: Session = Depends(get_db)):
    # Check if user already exists
    db_user = db.query(models.User).filter(models.User.email == user.email).first()
    if db_user:
        raise HTTPException(
            status_code=400,
            detail="Email already registered"
        )
    
    # Hash the password and create user
    hashed_password = auth_utils.get_password_hash(user.password)
    new_user = models.User(
        email=user.email,
        name=user.name,
        passwordHash=hashed_password,
        timezone=user.timezone
    )
    
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    return new_user

@router.post("/login", response_model=schemas.Token)
def login(user_credentials: schemas.UserCreate, db: Session = Depends(get_db)):
    # Note: Using UserCreate schema for login for simplicity here, 
    # but normally you'd use a dedicated Login schema (email/password only).
    
    db_user = db.query(models.User).filter(models.User.email == user_credentials.email).first()
    
    if not db_user or not auth_utils.verify_password(user_credentials.password, db_user.passwordHash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    # Create JWT
    access_token = auth_utils.create_access_token(
        data={"sub": db_user.email, "userId": db_user.userId}
    )
    
    return {"access_token": access_token, "token_type": "bearer"}
